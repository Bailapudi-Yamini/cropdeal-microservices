package com.cropdeal.orderservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "crop-service", path = "/crops")
public interface CropServiceClient {

    @GetMapping("/{id}")
    ApiWrapper getListingById(@PathVariable Long id);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiWrapper(boolean success, String message, CropListingSummary data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CropListingSummary(Long id, Long farmerId, Double pricePerUnit, String status) {}
}
