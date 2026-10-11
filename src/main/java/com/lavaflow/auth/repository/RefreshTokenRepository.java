package com.lavaflow.auth.repository;

import com.lavaflow.auth.entity.RefreshToken;
import com.lavaflow.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllByUser(@Param("user") User user);


    @Modifying
    @Query("delete from RefreshToken t where t.user.id in :userIds")
    int deleteAllByUserIdIn(@Param("userIds") Collection<UUID> userIds);
}
