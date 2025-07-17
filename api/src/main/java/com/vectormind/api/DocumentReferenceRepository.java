package com.vectormind.api;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentReferenceRepository extends JpaRepository<DocumentReference, String> {
    List<DocumentReference> findByUserId(String userId);
    Optional<DocumentReference> findByDocIdAndUserId(String docId, String userId);
}

