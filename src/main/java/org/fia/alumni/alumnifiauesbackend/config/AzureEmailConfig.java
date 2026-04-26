package org.fia.alumni.alumnifiauesbackend.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureEmailConfig {

    @Value("${azure.communication.connection-string}")
    private String connectionString;

    @Bean
    public EmailClient emailClient() {
        return new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}