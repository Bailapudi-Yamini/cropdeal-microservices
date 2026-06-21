package com.cropdeal.cropservice.repository;

import com.cropdeal.cropservice.entity.CropListing;
import com.cropdeal.cropservice.entity.CropStatus;
import com.cropdeal.cropservice.entity.CropType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CropListingRepository extends JpaRepository<CropListing, Long> {

    Page<CropListing> findByStatus(CropStatus status, Pageable pageable);

    List<CropListing> findByFarmerIdAndStatus(Long farmerId, CropStatus status);

    List<CropListing> findByFarmerId(Long farmerId);

    Page<CropListing> findByCropTypeAndStatus(CropType cropType, CropStatus status, Pageable pageable);

    @Query("SELECT c FROM CropListing c WHERE c.status = 'AVAILABLE' " +
           "AND (:cropType IS NULL OR c.cropType = :cropType) " +
           "AND (:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<CropListing> searchAvailable(@Param("cropType") CropType cropType,
                                      @Param("location") String location,
                                      Pageable pageable);

    @Modifying
    @Query("UPDATE CropListing c SET c.status = :status WHERE c.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") CropStatus status);

    @Query("SELECT COUNT(c) FROM CropListing c WHERE c.farmerId = :farmerId AND c.status = 'AVAILABLE'")
    long countActiveByFarmer(@Param("farmerId") Long farmerId);

    Optional<CropListing> findByIdAndFarmerId(Long id, Long farmerId);
}
