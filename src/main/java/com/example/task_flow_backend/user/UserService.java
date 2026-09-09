package com.example.task_flow_backend.user;

import com.example.task_flow_backend.common.PageResponse;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueStatuses;
import com.example.task_flow_backend.user.dto.UserDtos.UpdateUserRequest;
import com.example.task_flow_backend.user.dto.UserDtos.UserResponse;
import com.example.task_flow_backend.user.dto.UserDtos.WorkloadResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    public UserService(UserRepository userRepository, IssueRepository issueRepository) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String skill, Boolean active, Pageable pageable) {
        String skillFilter = StringUtils.hasText(skill) ? skill.trim() : null;
        return PageResponse.from(userRepository.search(skillFilter, active, pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(require(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = require(id);
        if (request.fullName() != null && StringUtils.hasText(request.fullName())) {
            user.setFullName(request.fullName().trim());
        }
        if (request.wipLimit() != null) {
            user.setWipLimit(request.wipLimit());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public WorkloadResponse workload(Long id) {
        User user = require(id);
        long open = issueRepository.countByAssigneeIdAndStatusIn(id, IssueStatuses.OPEN);
        return new WorkloadResponse(id, open, user.getWipLimit(), user.getAvgResolutionHours());
    }

    private User require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User " + id + " not found"));
    }
}
