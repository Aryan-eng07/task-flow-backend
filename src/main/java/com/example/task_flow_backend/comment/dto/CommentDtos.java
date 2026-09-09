package com.example.task_flow_backend.comment.dto;

import com.example.task_flow_backend.comment.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentDtos {

    private CommentDtos() {
    }

    public record CreateCommentRequest(@NotBlank @Size(max = 20000) String body) {
    }

    public record CommentResponse(
            Long id,
            Long issueId,
            Long authorId,
            String authorName,
            String body,
            Instant createdAt) {

        public static CommentResponse from(Comment c, String authorName) {
            return new CommentResponse(c.getId(), c.getIssueId(), c.getAuthorId(), authorName,
                    c.getBody(), c.getCreatedAt());
        }
    }
}
