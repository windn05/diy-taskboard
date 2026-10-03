package com.taskboard.user;

import com.taskboard.auth.AuthDtos.CreateUserRequest;
import com.taskboard.auth.AuthDtos.UserResponse;
import com.taskboard.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 계정 목록과 계정 생성. 공개 회원가입이 없어 계정은 여기서만 생성 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping
    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getName(), user.getRole().name()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUser(request);
    }
}
