package com.example.task_flow_backend.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Single choke point for board deltas. Services call this after a create,
 * status change or (re)assignment; it fans the delta out over STOMP.
 */
@Component
public class BoardEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BoardEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public BoardEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(Long projectId, BoardEvent event) {
        String destination = "/topic/projects/" + projectId + "/board";
        log.debug("board delta -> {} {}", destination, event);
        messagingTemplate.convertAndSend(destination, event);
    }
}
