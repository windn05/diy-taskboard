package com.taskboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskboard.domain.Card;
import com.taskboard.domain.Status;
import com.taskboard.domain.User;
import com.taskboard.domain.Workspace;
import com.taskboard.domain.WorkspaceMember;
import com.taskboard.domain.WorkspaceMember.WorkspaceRole;
import com.taskboard.dto.AuthDtos.LoginRequest;
import com.taskboard.dto.AuthDtos.CreateUserRequest;
import com.taskboard.repository.*;
import com.taskboard.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게스트 읽기 전용, 워크스페이스 멤버십, 관리자 전용 규칙이 실제 요청에서 지켜지는지 검증한다.
 * 권한은 화면이 아니라 서버에서 막혀야 하므로 REST 레벨에서 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionMatrixTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired ActivityLogRepository activityLogRepository;

    private String memberToken;
    private String outsiderToken;
    private String adminToken;
    private String guestToken;

    private Long visibleWorkspaceId;
    private Long hiddenWorkspaceId;
    private Long cardId;

    @BeforeEach
    void setUp() {
        activityLogRepository.deleteAll();
        commentRepository.deleteAll();
        cardRepository.deleteAll();
        memberRepository.deleteAll();
        workspaceRepository.deleteAll();
        statusRepository.deleteAll();
        userRepository.deleteAll();

        Long memberId = signup("member", "pw12345678", "멤버");
        signup("outsider", "pw12345678", "비멤버");
        Long adminId = signup("admin", "pw12345678", "관리자");
        promoteToAdmin(adminId);

        memberToken = login("member");
        outsiderToken = login("outsider");
        adminToken = login("admin");
        guestToken = authService.guestLogin().accessToken();

        Status status = statusRepository.save(Status.builder().name("할 일").order(1).build());
        visibleWorkspaceId = createWorkspace("공개 프로젝트", memberId, true);
        hiddenWorkspaceId = createWorkspace("숨김 프로젝트", memberId, false);
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(visibleWorkspaceId).userId(memberId).role(WorkspaceRole.OWNER).build());

        cardId = cardRepository.save(Card.builder()
                .workspaceId(visibleWorkspaceId).statusId(status.getId()).title("작업").type("Task")
                .labels(List.of("label-1"))
                .build()).getId();
    }

    // --- 게스트: 읽기만 가능 ---

    @Test
    void 게스트는_공개로_지정된_프로젝트만_조회한다() throws Exception {
        mockMvc.perform(auth(get("/workspaces"), guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("공개 프로젝트"));
    }

    @Test
    void 게스트는_비공개_프로젝트의_작업을_볼_수_없다() throws Exception {
        mockMvc.perform(auth(get("/workspaces/" + hiddenWorkspaceId + "/cards"), guestToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 멤버는_게스트_비공개_프로젝트도_자기_목록에서_본다() throws Exception {
        // visible은 게스트 공개 여부일 뿐이라, 멤버에게는 영향을 주지 않아야 한다.
        Long memberId = userRepository.findByUsername("member").orElseThrow().getId();
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(hiddenWorkspaceId).userId(memberId).role(WorkspaceRole.MEMBER).build());

        mockMvc.perform(auth(get("/workspaces"), memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 게스트는_멤버가_아니어도_노출된_프로젝트의_작업을_볼_수_있다() throws Exception {
        mockMvc.perform(auth(get("/workspaces/" + visibleWorkspaceId + "/cards"), guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 게스트는_작업을_생성할_수_없다() throws Exception {
        mockMvc.perform(auth(post("/workspaces/" + visibleWorkspaceId + "/cards"), guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"게스트가 만든 작업\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 게스트는_작업을_수정하거나_삭제할_수_없다() throws Exception {
        mockMvc.perform(auth(patch("/cards/" + cardId), guestToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"변경\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth(delete("/cards/" + cardId), guestToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 게스트는_댓글을_달_수_없다() throws Exception {
        mockMvc.perform(auth(post("/cards/" + cardId + "/comments"), guestToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"댓글\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 게스트는_관리자_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(auth(get("/admin/workspaces"), guestToken)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/admin/logs"), guestToken)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/admin/metrics"), guestToken)).andExpect(status().isForbidden());
    }

    // --- 일반 사용자 ---

    @Test
    void 멤버는_작업을_생성하고_삭제할_수_있다() throws Exception {
        String created = mockMvc.perform(auth(post("/workspaces/" + visibleWorkspaceId + "/cards"), memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"새 작업\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(auth(delete("/cards/" + newId), memberToken)).andExpect(status().isNoContent());
    }

    @Test
    void 멤버가_아니면_프로젝트의_작업을_볼_수_없다() throws Exception {
        mockMvc.perform(auth(get("/workspaces/" + visibleWorkspaceId + "/cards"), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 일반_사용자는_관리자_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(auth(get("/admin/logs"), memberToken)).andExpect(status().isForbidden());
    }

    // --- 관리자 ---

    @Test
    void 관리자는_숨김_프로젝트를_포함해_전체를_조회한다() throws Exception {
        mockMvc.perform(auth(get("/admin/workspaces"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void 관리자는_로그와_메트릭을_조회할_수_있다() throws Exception {
        mockMvc.perform(auth(get("/admin/logs"), adminToken)).andExpect(status().isOk());
        mockMvc.perform(auth(get("/admin/metrics"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()").value(60));
    }

    // --- 미인증 ---

    @Test
    void 토큰이_없으면_401을_돌려준다() throws Exception {
        // 403이면 프론트가 "권한 없음"과 "토큰 만료"를 구분하지 못해 로그인 화면으로 보낼 수 없다.
        mockMvc.perform(get("/workspaces")).andExpect(status().isUnauthorized());
    }

    @Test
    void 잘못된_토큰도_401이다() throws Exception {
        mockMvc.perform(auth(get("/workspaces"), "not-a-real-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void 인증은_됐지만_권한이_없으면_403이다() throws Exception {
        mockMvc.perform(auth(get("/admin/logs"), guestToken)).andExpect(status().isForbidden());
    }

    @Test
    void 일반_사용자는_프로젝트를_만들_수_없다() throws Exception {
        // 프로젝트 생성은 관리자 전용(POST /admin/workspaces)이다.
        mockMvc.perform(auth(post("/workspaces"), memberToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"몰래 만든 프로젝트\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 관리자는_자신이_만들지_않은_프로젝트의_멤버도_관리한다() throws Exception {
        // 워크스페이스 역할이 아니라 시스템 ADMIN 기준이라, 멤버가 아닌 프로젝트도 관리할 수 있어야 한다.
        mockMvc.perform(auth(post("/admin/workspaces/" + visibleWorkspaceId + "/members"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"outsider\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated());

        // 멤버로 추가되면 그 사용자의 프로젝트 목록에 나타난다.
        mockMvc.perform(auth(get("/workspaces"), outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("공개 프로젝트"));

        Long outsiderId = userRepository.findByUsername("outsider").orElseThrow().getId();
        mockMvc.perform(auth(delete("/admin/workspaces/" + visibleWorkspaceId + "/members/" + outsiderId), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(auth(get("/workspaces"), outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 일반_사용자는_멤버를_추가할_수_없다() throws Exception {
        mockMvc.perform(auth(post("/admin/workspaces/" + visibleWorkspaceId + "/members"), memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"outsider\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_프로젝트를_만들_수_있다() throws Exception {
        mockMvc.perform(auth(post("/admin/workspaces"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 프로젝트\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 게스트의_조회는_데이터를_만들지_않는다() throws Exception {
        // 전역 상태/타입 초기화가 조회 안에 있으면 게스트의 GET이 INSERT를 유발한다.
        statusRepository.deleteAll();

        mockMvc.perform(auth(get("/statuses"), guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(statusRepository.count()).isZero();
    }

    // --- helpers ---

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private Long signup(String username, String password, String name) {
        return authService.createUser(new CreateUserRequest(username, password, name, null)).id();
    }

    private void promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(User.SystemRole.ADMIN);
        userRepository.save(user);
    }

    private String login(String username) {
        return authService.login(new LoginRequest(username, "pw12345678")).accessToken();
    }

    private Long createWorkspace(String name, Long ownerId, boolean visible) {
        return workspaceRepository.save(
                Workspace.builder().name(name).ownerId(ownerId).visible(visible).build()).getId();
    }
}
