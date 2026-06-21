package com.cropdeal.adminservice.query.service;

import com.cropdeal.adminservice.command.model.AddOn;
import com.cropdeal.adminservice.command.model.AddOnRepository;
import com.cropdeal.adminservice.dto.response.AddOnResponse;
import com.cropdeal.adminservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * QUERY SIDE — read-only add-on queries.
 * Reads from the same AddOn table as the command side
 * (acceptable for simple CRUD entities that don't need a separate read model).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddOnQueryService {

    private final AddOnRepository addOnRepository;

    public List<AddOnResponse> getAll() {
        return addOnRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<AddOnResponse> getActive() {
        return addOnRepository.findAll().stream()
                .filter(AddOn::isActive).map(this::toResponse).toList();
    }

    public AddOnResponse getById(Long id) {
        return addOnRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AddOn", id));
    }

    private AddOnResponse toResponse(AddOn a) {
        return AddOnResponse.builder()
                .id(a.getId()).name(a.getName())
                .description(a.getDescription()).active(a.isActive())
                .createdAt(a.getCreatedAt()).build();
    }
}
