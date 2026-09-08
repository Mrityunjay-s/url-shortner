package com.personal.urlshort.repository;

import com.personal.urlshort.entities.Urls;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlRepository extends JpaRepository<Urls, UUID> {
    Optional<Urls> findByShortCode(String shortCode);

    Optional<Urls> findByLongUrl(String longUrl);

    @Modifying
    @Query("""
            DELETE FROM Urls u WHERE u.shortCode = :code
            """)
    int deleteByShortCode(@Param("code") String shortCode);
}
