package org.fia.alumni.alumnifiauesbackend.repository.catalog;

import org.fia.alumni.alumnifiauesbackend.entity.catalog.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByIsoCode(String isoCode);
}