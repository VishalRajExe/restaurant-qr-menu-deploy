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

    List<DiningTable> findByRestaurantIdAndIsDeletedFalseOrderByTableNumberAsc(Long restaurantId);

    List<DiningTable> findByRestaurantIdAndBranchIdAndIsDeletedFalseOrderByTableNumberAsc(Long restaurantId, Long branchId);

    Optional<DiningTable> findByRestaurantIdAndTableNumberAndIsDeletedFalse(Long restaurantId, String tableNumber);

    List<DiningTable> findByRestaurantIdAndStatusAndIsDeletedFalse(Long restaurantId, DiningTable.Status status);

    @Query("SELECT t FROM DiningTable t WHERE t.restaurant.id = :restaurantId AND t.isDeleted = false AND (t.tableNumber = :tableNum OR t.tableNumber = :altTableNum)")
    List<DiningTable> findByRestaurantIdAndTableNumberFuzzy(@Param("restaurantId") Long restaurantId,
                                                            @Param("tableNum") String tableNum,
                                                            @Param("altTableNum") String altTableNum);

    long countByRestaurantIdAndStatusAndIsDeletedFalse(Long restaurantId, DiningTable.Status status);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    default long countByRestaurantId(Long restaurantId) {
        return countByRestaurantIdAndIsDeletedFalse(restaurantId);
    }
}
