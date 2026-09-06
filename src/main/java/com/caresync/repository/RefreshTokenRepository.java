package com.caresync.repository;

import com.caresync.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.userEmail = :email AND r.isRevoked = false")
    void revokeAllByUserEmail(@Param("email") String email);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.token = :token")
    void revokeByToken(@Param("token") String token);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
