package com.kk.kblog.repository.site;

import com.kk.kblog.entity.site.AdminUserEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {

    @Modifying
    @Query("""
            update AdminUserEntity a
            set a.lastLoginAt = :at,
                a.lastLoginIp = :ip,
                a.lastLoginLocation = :location
            where a.id = :id
            """)
    int updateLastLogin(@Param("id") long id, @Param("at") Instant at, @Param("ip") String ip, @Param("location") String location);
}

