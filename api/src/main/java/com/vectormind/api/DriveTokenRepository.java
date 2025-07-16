package com.vectormind.api;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DriveTokenRepository extends JpaRepository<DriveToken, Long> {
    Optional<DriveToken> findByUserId(String userId);
}
