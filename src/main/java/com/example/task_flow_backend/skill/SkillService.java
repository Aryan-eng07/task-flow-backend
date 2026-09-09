package com.example.task_flow_backend.skill;

import com.example.task_flow_backend.common.exception.BusinessRuleException;
import com.example.task_flow_backend.skill.dto.SkillDtos.CreateSkillRequest;
import com.example.task_flow_backend.skill.dto.SkillDtos.ReplaceUserSkillsRequest;
import com.example.task_flow_backend.skill.dto.SkillDtos.SkillResponse;
import com.example.task_flow_backend.skill.dto.SkillDtos.UserSkillItem;
import com.example.task_flow_backend.skill.dto.SkillDtos.UserSkillView;
import com.example.task_flow_backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserRepository userRepository;

    public SkillService(SkillRepository skillRepository,
                        UserSkillRepository userSkillRepository,
                        UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills() {
        return skillRepository.findAllByOrderByNameAsc().stream().map(SkillResponse::from).toList();
    }

    @Transactional
    public SkillResponse createSkill(CreateSkillRequest request) {
        String name = request.name().trim().toLowerCase();
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("SKILL_EXISTS", "A skill named '" + name + "' already exists");
        }
        Skill skill = new Skill();
        skill.setName(name);
        return SkillResponse.from(skillRepository.save(skill));
    }

    @Transactional(readOnly = true)
    public List<UserSkillView> getUserSkills(Long userId) {
        requireUser(userId);
        List<UserSkill> rows = userSkillRepository.findByUserId(userId);
        Map<Long, String> names = skillRepository.findAllById(
                        rows.stream().map(UserSkill::getSkillId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(Skill::getId, Skill::getName));
        return rows.stream()
                .map(r -> new UserSkillView(r.getSkillId(), names.get(r.getSkillId()), r.getProficiency()))
                .toList();
    }

    /** Full replace of a user's skill set. */
    @Transactional
    public List<UserSkillView> replaceUserSkills(Long userId, ReplaceUserSkillsRequest request) {
        requireUser(userId);
        Map<Long, Skill> byId = skillRepository.findAllById(
                        request.skills().stream().map(UserSkillItem::skillId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(Skill::getId, Function.identity()));

        for (UserSkillItem item : request.skills()) {
            if (!byId.containsKey(item.skillId())) {
                throw new EntityNotFoundException("Skill " + item.skillId() + " not found");
            }
        }

        userSkillRepository.deleteByUserId(userId);
        userSkillRepository.flush();

        List<UserSkill> toSave = request.skills().stream().map(item -> {
            UserSkill us = new UserSkill();
            us.setUserId(userId);
            us.setSkillId(item.skillId());
            us.setProficiency(item.proficiency());
            return us;
        }).toList();
        userSkillRepository.saveAll(toSave);

        return toSave.stream()
                .map(r -> new UserSkillView(r.getSkillId(), byId.get(r.getSkillId()).getName(), r.getProficiency()))
                .toList();
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User " + userId + " not found");
        }
    }
}
