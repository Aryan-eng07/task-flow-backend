package com.example.task_flow_backend.user;

import com.example.task_flow_backend.common.PageResponse;
import com.example.task_flow_backend.user.dto.UserDtos.UpdateUserRequest;
import com.example.task_flow_backend.user.dto.UserDtos.UserResponse;
import com.example.task_flow_backend.user.dto.UserDtos.WorkloadResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LEAD')")
    public PageResponse<UserResponse> list(@RequestParam(required = false) String skill,
                                           @RequestParam(required = false) Boolean active,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return userService.search(skill, active, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @GetMapping("/{id}/workload")
    public WorkloadResponse workload(@PathVariable Long id) {
        return userService.workload(id);
    }
}
