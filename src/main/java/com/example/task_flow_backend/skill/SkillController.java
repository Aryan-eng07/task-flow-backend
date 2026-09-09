package com.example.task_flow_backend.skill;

import com.example.task_flow_backend.skill.dto.SkillDtos.CreateSkillRequest;
import com.example.task_flow_backend.skill.dto.SkillDtos.ReplaceUserSkillsRequest;
import com.example.task_flow_backend.skill.dto.SkillDtos.SkillResponse;
import com.example.task_flow_backend.skill.dto.SkillDtos.UserSkillView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/skills")
    public List<SkillResponse> listSkills() {
        return skillService.listSkills();
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SkillResponse createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return skillService.createSkill(request);
    }

    @GetMapping("/users/{id}/skills")
    public List<UserSkillView> getUserSkills(@PathVariable Long id) {
        return skillService.getUserSkills(id);
    }

    @PutMapping("/users/{id}/skills")
    @PreAuthorize("hasAnyRole('ADMIN','LEAD') or #id == principal.id")
    public List<UserSkillView> replaceUserSkills(@PathVariable Long id,
                                                 @Valid @RequestBody ReplaceUserSkillsRequest request) {
        return skillService.replaceUserSkills(id, request);
    }
}
