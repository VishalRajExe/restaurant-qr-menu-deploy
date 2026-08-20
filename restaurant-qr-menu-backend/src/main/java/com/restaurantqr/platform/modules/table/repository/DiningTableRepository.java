package com.restaurantqr.platform.modules.table.repository;

import com.restaurantqr.platform.modules.table.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByRestaurantIdOrderByTableNumberAsc(Long restaurantId);

    List<DiningTable> findByRestaurantIdAndBranchIdOrderByTableNumberAsc(Long restaurantId, Long branchId);

    Optional<DiningTable> findByRestaurantIdAndTableNumber(Long restaurantId, String tableNumber);

    List<DiningTable> findByRestaurantIdAndStatus(Long restaurantId, DiningTable.Status status);

    @Query("SELECT t FROM DiningTable t WHERE t.restaurant.id = :restaurantId AND (t.tableNumber = :tableNum OR t.tableNumber = :altTableNum)")
    List<DiningTable> findByRestaurantIdAndTableNumberFuzzy(@Param("restaurantId") Long restaurantId,
                                                            @Param("tableNum") String tableNum,
                                                            @Param("altTableNum") String altTableNum);

    long countByRestaurantIdAndStatus(Long restaurantId, DiningTable.Status status);

    long countByRestaurantId(Long restaurantId);
}
