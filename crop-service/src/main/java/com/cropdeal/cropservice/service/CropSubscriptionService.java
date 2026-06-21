package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.dto.SubscriptionRequest;
import com.cropdeal.cropservice.dto.SubscriptionResponse;

import java.util.List;

public interface CropSubscriptionService {

    SubscriptionResponse subscribe(Long dealerId, SubscriptionRequest request);

    void unsubscribe(Long subscriptionId, Long dealerId);

    List<SubscriptionResponse> getMySubscriptions(Long dealerId);
}
