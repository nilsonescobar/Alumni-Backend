package org.fia.alumni.alumnifiauesbackend.repository.profile;

import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

}