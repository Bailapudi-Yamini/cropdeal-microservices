package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.dto.SubscriptionRequest;
import com.cropdeal.cropservice.dto.SubscriptionResponse;
import com.cropdeal.cropservice.entity.CropSubscription;
import com.cropdeal.cropservice.exception.DuplicateEntryException;
import com.cropdeal.cropservice.exception.UnauthorizedActionException;
import com.cropdeal.cropservice.repository.CropSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CropSubscriptionServiceImpl implements CropSubscriptionService {

    private final CropSubscriptionRepository subscriptionRepository;
    private final CropMapper cropMapper;

    @Override
    public SubscriptionResponse subscribe(Long dealerId, SubscriptionRequest request) {
        String location = request.getPreferredLocation();

        if (subscriptionRepository.existsByDealerIdAndCropTypeAndPreferredLocation(
                dealerId, request.getCropType(), location)) {
            throw new DuplicateEntryException(
                    "Already subscribed to " + request.getCropType() + " in " + location);
        }

        CropSubscription subscription = CropSubscription.builder()
                .dealerId(dealerId)
                .cropType(request.getCropType())
                .preferredLocation(location)
                .active(true)
                .build();

        return cropMapper.toSubscriptionResponse(subscriptionRepository.save(subscription));
    }

    @Override
    public void unsubscribe(Long subscriptionId, Long dealerId) {
        CropSubscription subscription = subscriptionRepository.findByIdAndDealerId(subscriptionId, dealerId)
                .orElseThrow(() -> new UnauthorizedActionException(
                        "Subscription not found or you do not own it"));
        subscription.setActive(false);
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(Long dealerId) {
        return subscriptionRepository.findByDealerIdAndActiveTrue(dealerId).stream()
                .map(cropMapper::toSubscriptionResponse)
                .toList();
    }
}
