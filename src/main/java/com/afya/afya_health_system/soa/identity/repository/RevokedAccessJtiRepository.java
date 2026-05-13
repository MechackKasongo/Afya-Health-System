package com.afya.afya_health_system.soa.identity.repository;

import com.afya.afya_health_system.soa.identity.model.RevokedAccessJti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedAccessJtiRepository extends JpaRepository<RevokedAccessJti, String> {

    @Modifying
    @Query("delete from RevokedAccessJti r where r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
