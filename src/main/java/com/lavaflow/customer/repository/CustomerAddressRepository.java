package com.lavaflow.customer.repository;

import com.lavaflow.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(UUID customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<CustomerAddress> findByCustomerIdAndDefaultAddressTrue(UUID customerId);

    Optional<CustomerAddress> findFirstByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    long countByCustomerId(UUID customerId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update CustomerAddress a
            set a.defaultAddress = false, a.updatedAt = :now
            where a.customer.id = :customerId and a.defaultAddress = true
            """)
    int clearDefaults(@Param("customerId") UUID customerId, @Param("now") Instant now);
}
