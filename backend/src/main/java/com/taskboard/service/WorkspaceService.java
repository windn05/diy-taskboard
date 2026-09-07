package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.User;
import com.taskboard.domain.User.SystemRole;
import com.taskboard.domain.Workspace;
import com.taskboard.domain.WorkspaceMember;
import com.taskboard.domain.WorkspaceMember.WorkspaceRole;
import com.taskboard.dto.WorkspaceDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.exception.AccessDeniedException;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.ActivityLogRepository;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.CommentRepository;
import com.taskboard.repository.ReleaseRepository;
import com.taskboard.repository.UserRepository;
import com.taskboard.repository.WorkspaceMemberRepository;
import com.taskboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CommentRepository commentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ReleaseRepository releaseRepository;
    private final NotificationService notificationService;

    @Transactional
    public WorkspaceResponse create(Long userId, CreateWorkspaceRequest request) {
        Workspace workspace = Workspace.builder().name(request.name()).ownerId(userId).build();
        workspaceRepository.save(workspace);
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(userId)
                .role(WorkspaceRole.OWNER)
                .build());
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getOwnerId(), WorkspaceRole.OWNER.name());
    }

    /**
     * 프로젝트 접근은 멤버십으로 정한다. 관리자도 예외가 아니며, 전체 목록은 관리자 앱에서만 본다.
     * 게스트는 멤버가 될 수 없으므로 관리자가 "게스트 공개"로 지정한 프로젝트({@code visible})만 볼 수 있다.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(CurrentUser user) {
        if (user.isGuest()) {
            return workspaceRepository.findAll().stream()
                    .filter(Workspace::isVisible)
                    .map(w -> new WorkspaceResponse(w.getId(), w.getName(), w.getOwnerId(), SystemRole.GUEST.name()))
                    .toList();
        }
        List<WorkspaceMember> memberships = memberRepository.findByUserId(user.getId());
        Map<Long, Workspace> workspaces = findAllByIdAsMap(
                workspaceRepository, memberships.stream().map(WorkspaceMember::getWorkspaceId).toList(), Workspace::getId);

        return memberships.stream()
                .map(m -> {
                    // 멤버는 게스트 공개 여부와 무관하게 자기 프로젝트를 본다.
                    Workspace w = workspaces.get(m.getWorkspaceId());
                    return w != null
                            ? new WorkspaceResponse(w.getId(), w.getName(), w.getOwnerId(), m.getRole().name())
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<AdminWorkspaceResponse> listAllForAdmin() {
        return workspaceRepository.findAll().stream()
                .map(w -> new AdminWorkspaceResponse(w.getId(), w.getName(), w.getOwnerId(), w.isVisible()))
                .toList();
    }

    @Transactional
    public AdminWorkspaceResponse updateAsAdmin(Long workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));
        if (request.name() != null && !request.name().isBlank()) workspace.setName(request.name());
        if (request.visible() != null) workspace.setVisible(request.visible());
        return new AdminWorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getOwnerId(), workspace.isVisible());
    }

    @Transactional
    public void deleteAsAdmin(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new EntityNotFoundException("워크스페이스를 찾을 수 없습니다.");
        }
        List<Card> cards = cardRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
        List<Long> cardIds = cards.stream().map(Card::getId).toList();
        if (!cardIds.isEmpty()) {
            notificationService.deleteByCardIds(cardIds);
            activityLogRepository.deleteByCardIdIn(cardIds);
            commentRepository.deleteByCardIdIn(cardIds);
            cardRepository.deleteAll(cards);
        }
        releaseRepository.deleteByWorkspaceId(workspaceId);
        memberRepository.deleteByWorkspaceId(workspaceId);
        workspaceRepository.deleteById(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(CurrentUser user, Long workspaceId) {
        requireReadAccess(user, workspaceId);
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        Map<Long, User> users = findAllByIdAsMap(
                userRepository, members.stream().map(WorkspaceMember::getUserId).toList(), User::getId);

        return members.stream()
                .map(m -> {
                    User u = users.get(m.getUserId());
                    return u != null ? new MemberResponse(u.getId(), u.getUsername(), u.getName(), m.getRole().name()) : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /** 연관 엔티티를 한 번에 조회해 id로 찾을 수 있게 만든다 (건별 findById로 인한 N+1 방지). */
    private <T> Map<Long, T> findAllByIdAsMap(JpaRepository<T, Long> repository, List<Long> ids, Function<T, Long> idOf) {
        if (ids.isEmpty()) return Map.of();
        return repository.findAllById(ids).stream().collect(Collectors.toMap(idOf, entity -> entity));
    }

    /**
     * 멤버 관리는 시스템 관리자만 한다. 워크스페이스 역할(OWNER/ADMIN)이 아니라 시스템 ADMIN 기준이므로,
     * 관리자는 자신이 만들지 않은 프로젝트의 멤버도 관리할 수 있다.
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembersAsAdmin(Long workspaceId) {
        requireWorkspace(workspaceId);
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        Map<Long, User> users = findAllByIdAsMap(
                userRepository, members.stream().map(WorkspaceMember::getUserId).toList(), User::getId);

        return members.stream()
                .map(m -> {
                    User u = users.get(m.getUserId());
                    return u != null ? new MemberResponse(u.getId(), u.getUsername(), u.getName(), m.getRole().name()) : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public MemberResponse addMemberAsAdmin(Long workspaceId, AddMemberRequest request) {
        requireWorkspace(workspaceId);
        User target = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        if (memberRepository.findByWorkspaceIdAndUserId(workspaceId, target.getId()).isPresent()) {
            throw new IllegalArgumentException("이미 프로젝트 멤버입니다.");
        }
        WorkspaceRole role = WorkspaceRole.valueOf(request.role().toUpperCase());
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(target.getId())
                .role(role)
                .build());
        return new MemberResponse(target.getId(), target.getUsername(), target.getName(), role.name());
    }

    @Transactional
    public void removeMemberAsAdmin(Long workspaceId, Long userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트 멤버가 아닙니다."));
        memberRepository.delete(member);
    }

    private void requireWorkspace(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new EntityNotFoundException("워크스페이스를 찾을 수 없습니다.");
        }
    }

    /** 조회 권한. 멤버는 멤버십으로, 게스트는 "게스트 공개" 지정으로 판단한다. */
    public void requireReadAccess(CurrentUser user, Long workspaceId) {
        if (!user.isGuest()) {
            requireMember(user.getId(), workspaceId);
            return;
        }
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));
        if (!workspace.isVisible()) {
            throw new AccessDeniedException("게스트에게 공개되지 않은 프로젝트입니다.");
        }
    }

    public WorkspaceMember requireMember(Long userId, Long workspaceId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("워크스페이스 멤버가 아닙니다."));
    }

    public void requireRole(Long userId, Long workspaceId, WorkspaceRole... allowed) {
        WorkspaceMember member = requireMember(userId, workspaceId);
        for (WorkspaceRole role : allowed) {
            if (member.getRole() == role) return;
        }
        throw new AccessDeniedException("권한이 없습니다.");
    }
}
