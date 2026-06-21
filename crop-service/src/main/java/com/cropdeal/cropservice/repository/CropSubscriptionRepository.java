package com.cropdeal.cropservice.repository;

import com.cropdeal.cropservice.entity.CropSubscription;
import com.cropdeal.cropservice.entity.CropType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CropSubscriptionRepository extends JpaRepository<CropSubscription, Long> {

    List<CropSubscription> findByDealerIdAndActiveTrue(Long dealerId);

    boolean existsByDealerIdAndCropTypeAndPreferredLocation(Long dealerId, CropType cropType,
                                                            String preferredLocation);

    Optional<CropSubscription> findByIdAndDealerId(Long id, Long dealerId);

    /**
     * Find all active dealers subscribed to a given crop type,
     * optionally matching on location — used by notification fanout after crop.posted.
     */
    @Query("SELECT s FROM CropSubscription s WHERE s.active = true AND s.cropType = :cropType " +
           "AND (:location IS NULL OR LOWER(s.preferredLocation) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<CropSubscription> findActiveSubscribers(@Param("cropType") CropType cropType,
                                                 @Param("location") String location);
}
