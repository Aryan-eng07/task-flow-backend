package com.example.task_flow_backend.comment;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.comment.dto.CommentDtos.CommentResponse;
import com.example.task_flow_backend.comment.dto.CommentDtos.CreateCommentRequest;
import com.example.task_flow_backend.common.exception.ForbiddenOperationException;
import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueService;
import com.example.task_flow_backend.project.ProjectService;
import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueService issueService;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          IssueService issueService,
                          ProjectService projectService,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.issueService = issueService;
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(String issueKey, UserPrincipal principal) {
        Issue issue = issueService.require(issueKey);
        projectService.assertMember(issue.getProjectId(), principal);

        List<Comment> comments = commentRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        Map<Long, String> names = nameLookup(comments);
        return comments.stream()
                .map(c -> CommentResponse.from(c, names.get(c.getAuthorId())))
                .toList();
    }

    @Transactional
    public CommentResponse add(String issueKey, CreateCommentRequest request, UserPrincipal principal) {
        Issue issue = issueService.require(issueKey);
        projectService.assertMember(issue.getProjectId(), principal);

        Comment comment = new Comment();
        comment.setIssueId(issue.getId());
        comment.setAuthorId(principal.id());
        comment.setBody(request.body().trim());
        commentRepository.save(comment);

        String authorName = userRepository.findById(principal.id()).map(User::getFullName).orElse(null);
        return CommentResponse.from(comment, authorName);
    }

    @Transactional
    public void delete(Long commentId, UserPrincipal principal) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment " + commentId + " not found"));
        if (principal.role() != Role.ADMIN && !principal.id().equals(comment.getAuthorId())) {
            throw new ForbiddenOperationException("Only the author or an admin can delete this comment");
        }
        commentRepository.delete(comment);
    }

    private Map<Long, String> nameLookup(List<Comment> comments) {
        List<Long> ids = comments.stream().map(Comment::getAuthorId).distinct().toList();
        return userRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
    }
}
