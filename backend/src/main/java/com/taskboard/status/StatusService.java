package com.taskboard.status;

import com.taskboard.card.CardRepository;
import com.taskboard.global.exception.EntityNotFoundException;
import com.taskboard.status.StatusDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 작업 상태 관리 */
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
        // 새 상태는 맨 오른쪽 컬럼으로 추가
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

    /*************************************************************************
     * 목적 : 작업 상태 삭제 (작업이 남아 있으면 거부)
     * 이유 : 남은 작업이 갈 곳 없는 상태가 되지 않게 함
     * 파라미터
     * - statusId : 상태 id
     * 반환
     * -
     *************************************************************************/
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
