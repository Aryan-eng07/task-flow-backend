package com.example.task_flow_backend.comment;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.comment.dto.CommentDtos.CommentResponse;
import com.example.task_flow_backend.comment.dto.CommentDtos.CreateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/issues/{key}/comments")
    public List<CommentResponse> list(@PathVariable String key,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.list(key, principal);
    }

    @PostMapping("/issues/{key}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable String key,
                               @Valid @RequestBody CreateCommentRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.add(key, request, principal);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(id, principal);
    }
}
