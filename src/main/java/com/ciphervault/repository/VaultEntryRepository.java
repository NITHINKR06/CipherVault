package com.ciphervault.repository;

import com.ciphervault.model.VaultEntry;
import com.ciphervault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VaultEntryRepository extends JpaRepository<VaultEntry, Long> {

    List<VaultEntry> findByUserOrderByCreatedAtDesc(User user);

    List<VaultEntry> findByUserAndCategory(User user, String category);

    List<VaultEntry> findByUserAndIsBreached(User user, boolean isBreached);

    @Query("SELECT COUNT(v) FROM VaultEntry v WHERE v.user = :user AND v.isBreached = true")
    long countBreachedByUser(User user);

    @Query("SELECT COUNT(v) FROM VaultEntry v WHERE v.user = :user")
    long countByUser(User user);

    @Query("SELECT AVG(v.strengthScore) FROM VaultEntry v WHERE v.user = :user")
    Double avgStrengthByUser(User user);
}
