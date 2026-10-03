package com.taskboard.workspace;

import com.taskboard.card.Card;
import com.taskboard.card.CardRepository;
import com.taskboard.comment.CommentRepository;
import com.taskboard.global.exception.AccessDeniedException;
import com.taskboard.global.exception.EntityNotFoundException;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.release.ReleaseRepository;
import com.taskboard.user.User;
import com.taskboard.user.User.SystemRole;
import com.taskboard.user.UserRepository;
import com.taskboard.workspace.WorkspaceDtos.*;
import com.taskboard.workspace.WorkspaceMember.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 프로젝트·멤버 관리와 프로젝트 단위 권한 검사. 다른 서비스는 여기의 require* 메서드로 권한 확인 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CommentRepository commentRepository;
    private final ReleaseRepository releaseRepository;

    /** 만든 사람은 OWNER 멤버로 자동 등록 */
    @Transactional
    public WorkspaceResponse create(Long userId, CreateWorkspaceRequest request) {
        Workspace workspace = Workspace.builder().name(request.name()).ownerId(userId).build();
        workspaceRepository.save(workspace);
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(userId)
                .role(WorkspaceRole.OWNER)
                .build());
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), WorkspaceRole.OWNER.name());
    }

    /**
     * 프로젝트 접근은 멤버십 기준. 관리자도 예외가 아니며, 전체 목록은 관리자 앱에서만 조회.
     * 게스트는 멤버가 될 수 없으므로 {@link #openToGuest} 조건을 만족하는 프로젝트만 조회 가능
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMine(CurrentUser user) {
        if (user.isGuest()) {
            Set<Long> hasRealMember = Set.copyOf(
                    memberRepository.findWorkspaceIdsWithMemberRoleOtherThan(SystemRole.ADMIN));
            return workspaceRepository.findAll().stream()
                    .filter(Workspace::isVisible)
                    .filter(w -> hasRealMember.contains(w.getId()))
                    .map(w -> new WorkspaceResponse(w.getId(), w.getName(), SystemRole.GUEST.name()))
                    .toList();
        }
        List<WorkspaceMember> memberships = memberRepository.findByUserId(user.getId());
        Map<Long, Workspace> workspaces = findAllByIdAsMap(
                workspaceRepository, memberships.stream().map(WorkspaceMember::getWorkspaceId).toList(), Workspace::getId);

        return memberships.stream()
                .map(m -> {
                    // 멤버는 게스트 공개 여부와 무관하게 자기 프로젝트 조회 가능
                    Workspace w = workspaces.get(m.getWorkspaceId());
                    return w != null
                            ? new WorkspaceResponse(w.getId(), w.getName(), m.getRole().name())
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<AdminWorkspaceResponse> listAllForAdmin() {
        return workspaceRepository.findAll().stream()
                .map(w -> new AdminWorkspaceResponse(w.getId(), w.getName(), w.isVisible()))
                .toList();
    }

    @Transactional
    public AdminWorkspaceResponse updateAsAdmin(Long workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));
        if (request.name() != null && !request.name().isBlank()) workspace.setName(request.name());
        if (request.visible() != null) workspace.setVisible(request.visible());
        return new AdminWorkspaceResponse(workspace.getId(), workspace.getName(), workspace.isVisible());
    }

    /** 연관관계 cascade가 없으므로 딸린 데이터를 참조 순서대로 직접 삭제 */
    @Transactional
    public void deleteAsAdmin(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new EntityNotFoundException("워크스페이스를 찾을 수 없습니다.");
        }
        List<Card> cards = cardRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
        List<Long> cardIds = cards.stream().map(Card::getId).toList();
        if (!cardIds.isEmpty()) {
            commentRepository.deleteByCardIdIn(cardIds);
            cardRepository.deleteAll(cards);
        }
        releaseRepository.deleteByWorkspaceId(workspaceId);
        memberRepository.deleteByWorkspaceId(workspaceId);
        workspaceRepository.deleteById(workspaceId);
    }

    /** 연관 엔티티를 한 번에 조회해 id로 찾을 수 있게 변환 (건별 findById로 인한 N+1 방지) */
    private <T> Map<Long, T> findAllByIdAsMap(JpaRepository<T, Long> repository, List<Long> ids, Function<T, Long> idOf) {
        if (ids.isEmpty()) return Map.of();
        return repository.findAllById(ids).stream().collect(Collectors.toMap(idOf, entity -> entity));
    }

    /**
     * 멤버 관리는 시스템 관리자 전용. 워크스페이스 역할(OWNER/ADMIN)이 아니라 시스템 ADMIN 기준이므로,
     * 관리자는 자신이 만들지 않은 프로젝트의 멤버도 관리 가능
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

    /** 조회 권한. 멤버는 멤버십으로, 게스트는 {@link #openToGuest} 조건으로 판단 */
    public void requireReadAccess(CurrentUser user, Long workspaceId) {
        if (!user.isGuest()) {
            requireMember(user.getId(), workspaceId);
            return;
        }
        openToGuest(workspaceId);
    }

    /**
     * 게스트가 볼 수 있는 프로젝트의 조건. 둘 다 만족해야 함
     * <ol>
     *   <li>관리자가 "게스트 공개"로 지정했을 것({@code visible})</li>
     *   <li>관리자 외의 멤버가 한 명이라도 있을 것 — 관리자 혼자 쓰는 프로젝트는 아직 공개할 단계가 아님</li>
     * </ol>
     */
    private void openToGuest(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));
        if (!workspace.isVisible()) {
            throw new AccessDeniedException("게스트에게 공개되지 않은 프로젝트입니다.");
        }
        if (!memberRepository.existsMemberRoleOtherThan(workspaceId, SystemRole.ADMIN)) {
            throw new AccessDeniedException("관리자만 참여 중인 프로젝트입니다.");
        }
    }

    /** 쓰기 권한. 게스트는 멤버가 될 수 없으므로 항상 거부 */
    public WorkspaceMember requireMember(Long userId, Long workspaceId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("워크스페이스 멤버가 아닙니다."));
    }
}
