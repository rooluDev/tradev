package com.tradev.domain.item.repository;

import com.tradev.domain.item.entity.Item;
import com.tradev.domain.item.entity.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("SELECT i FROM Item i JOIN FETCH i.seller JOIN FETCH i.category WHERE i.id = :id AND i.hidden = false")
    Optional<Item> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(i) FROM Item i WHERE i.seller.id = :sellerId AND i.status NOT IN ('COMPLETED') AND i.hidden = false")
    long countActiveBySeller(@Param("sellerId") Long sellerId);
}
