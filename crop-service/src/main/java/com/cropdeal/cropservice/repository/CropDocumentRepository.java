package com.cropdeal.cropservice.repository;

import com.cropdeal.cropservice.entity.CropDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CropDocumentRepository extends MongoRepository<CropDocument, String> {

    Optional<CropDocument> findByListingId(Long listingId);

    Page<CropDocument> findByStatus(String status, Pageable pageable);

    Page<CropDocument> findByCropTypeAndStatus(String cropType, String status, Pageable pageable);

    List<CropDocument> findByFarmerIdAndStatus(Long farmerId, String status);

    // Full-text style search on cropName and location
    @Query("{ 'status': 'AVAILABLE', $or: [ " +
           "{ 'cropName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'location': { $regex: ?0, $options: 'i' } } ] }")
    Page<CropDocument> fullTextSearch(String keyword, Pageable pageable);
}
