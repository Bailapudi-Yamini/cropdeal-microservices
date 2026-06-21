package com.cropdeal.adminservice.command.service;

import com.cropdeal.adminservice.command.model.AddOn;
import com.cropdeal.adminservice.command.model.AddOnRepository;
import com.cropdeal.adminservice.dto.request.AddOnRequest;
import com.cropdeal.adminservice.dto.response.AddOnResponse;
import com.cropdeal.adminservice.exception.DuplicateEntryException;
import com.cropdeal.adminservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddOnCommandService {

    private final AddOnRepository addOnRepository;

    public AddOnResponse create(AddOnRequest request) {
        if (addOnRepository.existsByName(request.getName())) {
            throw new DuplicateEntryException("AddOn already exists: " + request.getName());
        }
        AddOn addOn = AddOn.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();
        return toResponse(addOnRepository.save(addOn));
    }

    public AddOnResponse update(Long id, AddOnRequest request) {
        AddOn addOn = findById(id);
        addOn.setName(request.getName());
        addOn.setDescription(request.getDescription());
        return toResponse(addOnRepository.save(addOn));
    }

    public void toggleActive(Long id) {
        AddOn addOn = findById(id);
        addOn.setActive(!addOn.isActive());
        addOnRepository.save(addOn);
    }

    public void delete(Long id) {
        if (!addOnRepository.existsById(id)) {
            throw new ResourceNotFoundException("AddOn", id);
        }
        addOnRepository.deleteById(id);
    }

    private AddOn findById(Long id) {
        return addOnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AddOn", id));
    }

    private AddOnResponse toResponse(AddOn a) {
        return AddOnResponse.builder()
                .id(a.getId()).name(a.getName())
                .description(a.getDescription()).active(a.isActive())
                .createdAt(a.getCreatedAt()).build();
    }
}
