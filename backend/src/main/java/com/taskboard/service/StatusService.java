package com.taskboard.service;

import com.taskboard.domain.Status;
import com.taskboard.dto.StatusDtos.*;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;
    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public List<StatusResponse> list() {
        return statusRepository.findAllByOrderByOrderAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public StatusResponse create(StatusRequest request) {
        int order = statusRepository.findAllByOrderByOrderAsc().size();
        Status status = Status.builder().name(request.name()).order(order).build();
        statusRepository.save(status);
        return toResponse(status);
    }

    @Transactional
    public StatusResponse rename(Long statusId, StatusRequest request) {
        Status status = getStatus(statusId);
        status.setName(request.name());
        return toResponse(status);
    }

    @Transactional
    public void delete(Long statusId) {
        getStatus(statusId);
        if (!cardRepository.findByStatusId(statusId).isEmpty()) {
            throw new IllegalArgumentException("작업이 남아있는 상태는 삭제할 수 없습니다.");
        }
        statusRepository.deleteById(statusId);
    }

    private Status getStatus(Long statusId) {
        return statusRepository.findById(statusId).orElseThrow(() -> new EntityNotFoundException("상태를 찾을 수 없습니다."));
    }

    private StatusResponse toResponse(Status status) {
        return new StatusResponse(status.getId(), status.getName(), status.getOrder());
    }
}
