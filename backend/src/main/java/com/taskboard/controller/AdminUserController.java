package com.taskboard.controller;

import com.taskboard.dto.AuthDtos.CreateUserRequest;
import com.taskboard.dto.AuthDtos.UserResponse;
import com.taskboard.repository.UserRepository;
import com.taskboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 계정 목록과 계정 생성. 프로젝트 멤버로 추가할 사용자를 여기서 만든다. */
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
