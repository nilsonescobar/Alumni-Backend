package org.fia.alumni.alumnifiauesbackend.service.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.fia.alumni.alumnifiauesbackend.entity.security.UserMfa;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.security.UserMfaRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserMfaRepository mfaRepository;
    private final UserRepository userRepository;

    private static final String ISSUER = "Alumni FIA UES";
    private static final int SECRET_SIZE = 20;
    private static final int BACKUP_CODES_COUNT = 10;

    public Map<String, Object> enableMfa(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if MFA already enabled
        Optional<UserMfa> existingMfa = mfaRepository.findByUserId(userId);
        if (existingMfa.isPresent() && existingMfa.get().getIsEnabled()) {
            throw new BadRequestException("MFA is already enabled");
        }

        // Generate secret key
        String secretKey = generateSecretKey();

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();
        String backupCodesJson = String.join(",", backupCodes);

        // Save or update MFA configuration
        UserMfa mfa;
        if (existingMfa.isPresent()) {
            mfa = existingMfa.get();
            mfa.setSecretKey(secretKey);
            mfa.setBackupCodes(backupCodesJson);
        } else {
            mfa = UserMfa.builder()
                    .userId(userId)
                    .secretKey(secretKey)
                    .backupCodes(backupCodesJson)
                    .isEnabled(false) // Not enabled until verified
                    .build();
        }

        mfaRepository.save(mfa);

        // Generate QR code URL
        String qrCodeUrl = generateQRCodeUrl(user.getEmail(), secretKey);

        Map<String, Object> response = new HashMap<>();
        response.put("secretKey", secretKey);
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("backupCodes", backupCodes);
        response.put("message", "Scan the QR code with your authenticator app and verify the code");

        return response;
    }

    @Transactional
    public boolean verifyAndEnableMfa(Long userId, String code) {
        UserMfa mfa = mfaRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA not configured"));

        if (verifyCode(mfa.getSecretKey(), code)) {
            mfa.setIsEnabled(true);
            mfa.setEnabledAt(LocalDateTime.now());
            mfaRepository.save(mfa);

            log.info("MFA enabled successfully for user: {}", userId);
            return true;
        }

        throw new BadRequestException("Invalid MFA code");
    }

    @Transactional
    public void disableMfa(Long userId) {
        UserMfa mfa = mfaRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA not configured"));

        mfaRepository.delete(mfa);
        log.info("MFA disabled for user: {}", userId);
    }

    public boolean verifyMfaCode(Long userId, String code) {
        UserMfa mfa = mfaRepository.findByUserId(userId)
                .orElse(null);

        if (mfa == null || !mfa.getIsEnabled()) {
            return false;
        }

        // Try verifying with TOTP code
        if (verifyCode(mfa.getSecretKey(), code)) {
            return true;
        }

        // Try verifying with backup code
        return verifyBackupCode(mfa, code);
    }

    public boolean isMfaEnabled(Long userId) {
        return mfaRepository.existsByUserIdAndIsEnabledTrue(userId);
    }

    private boolean verifyCode(String secretKey, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000 / 30;

            // Check current time window and previous/next windows (to account for time drift)
            for (int i = -1; i <= 1; i++) {
                String expectedCode = generateTOTP(secretKey, currentTime + i);
                if (expectedCode.equals(code)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }

    private boolean verifyBackupCode(UserMfa mfa, String code) {
        if (mfa.getBackupCodes() == null) {
            return false;
        }

        List<String> backupCodes = new ArrayList<>(Arrays.asList(mfa.getBackupCodes().split(",")));

        if (backupCodes.remove(code)) {
            // Remove used backup code
            mfa.setBackupCodes(String.join(",", backupCodes));
            mfaRepository.save(mfa);

            log.info("Backup code used for user: {}", mfa.getUserId());
            return true;
        }

        return false;
    }

    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            int code = 100000 + random.nextInt(900000); // 6-digit codes
            codes.add(String.valueOf(code));
        }

        return codes;
    }

    private String generateQRCodeUrl(String email, String secretKey) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                ISSUER.replace(" ", "%20"),
                email,
                secretKey,
                ISSUER.replace(" ", "%20")
        );
    }

    private String generateTOTP(String secretKey, long timeIndex)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);

        byte[] timeBytes = new byte[8];
        long time = timeIndex;
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (time & 0xFF);
            time >>= 8;
        }

        SecretKeySpec signKey = new SecretKeySpec(bytes, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(timeBytes);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % 1000000;

        return String.format("%06d", otp);
    }

    public byte[] generateQRCodeImage(String qrCodeUrl) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        return outputStream.toByteArray();
    }
}