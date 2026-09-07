package com.personal.urlshort.repository;

import com.personal.urlshort.entities.Urls;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlRepository extends JpaRepository<Urls, UUID> {
    Optional<Urls> findByShortCode(String shortCode);
    Optional<Urls> findByLongUrl(String longUrl);
}
