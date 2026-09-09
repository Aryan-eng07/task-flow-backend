package com.example.task_flow_backend.config;

import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueRequiredSkill;
import com.example.task_flow_backend.issue.IssueRequiredSkillRepository;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.issue.IssueType;
import com.example.task_flow_backend.issue.Priority;
import com.example.task_flow_backend.project.Project;
import com.example.task_flow_backend.project.ProjectMember;
import com.example.task_flow_backend.project.ProjectMemberRepository;
import com.example.task_flow_backend.project.ProjectRepository;
import com.example.task_flow_backend.skill.Skill;
import com.example.task_flow_backend.skill.SkillRepository;
import com.example.task_flow_backend.skill.UserSkill;
import com.example.task_flow_backend.skill.UserSkillRepository;
import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo data so the board is not empty in screenshots. Enable with
 * {@code --spring.profiles.active=seed}; it is a no-op if users already exist.
 */
@Component
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final IssueRepository issueRepository;
    private final IssueRequiredSkillRepository requiredSkillRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, SkillRepository skillRepository,
                      UserSkillRepository userSkillRepository, ProjectRepository projectRepository,
                      ProjectMemberRepository memberRepository, IssueRepository issueRepository,
                      IssueRequiredSkillRepository requiredSkillRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.issueRepository = issueRepository;
        this.requiredSkillRepository = requiredSkillRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("seed skipped: {} users already present", userRepository.count());
            return;
        }
        log.info("seeding demo data");

        List<String> skillNames = List.of("java", "flutter", "mysql", "devops", "react", "python");
        Map<String, Skill> skills = new java.util.HashMap<>();
        for (String name : skillNames) {
            Skill s = new Skill();
            s.setName(name);
            skills.put(name, skillRepository.save(s));
        }

        String pw = passwordEncoder.encode("password123");
        User admin = user("admin@taskflow.dev", "Ada Admin", Role.ADMIN, 5, pw);
        User lead = user("lead@taskflow.dev", "Lena Lead", Role.LEAD, 6, pw);
        User m1 = user("java.dev@taskflow.dev", "Raj Backend", Role.MEMBER, 5, pw);
        User m2 = user("flutter.dev@taskflow.dev", "Mei Mobile", Role.MEMBER, 4, pw);
        User m3 = user("ops@taskflow.dev", "Sam Ops", Role.MEMBER, 5, pw);
        User m4 = user("fullstack@taskflow.dev", "Nia Fullstack", Role.MEMBER, 7, pw);
        List<User> all = List.of(admin, lead, m1, m2, m3, m4);
        userRepository.saveAll(all);

        addSkill(lead, skills.get("java"), 4);
        addSkill(lead, skills.get("mysql"), 3);
        addSkill(m1, skills.get("java"), 5);
        addSkill(m1, skills.get("mysql"), 4);
        addSkill(m2, skills.get("flutter"), 5);
        addSkill(m2, skills.get("react"), 3);
        addSkill(m3, skills.get("devops"), 5);
        addSkill(m3, skills.get("mysql"), 3);
        addSkill(m4, skills.get("java"), 3);
        addSkill(m4, skills.get("flutter"), 3);
        addSkill(m4, skills.get("react"), 4);
        addSkill(m4, skills.get("python"), 3);

        Project project = new Project();
        project.setKeyCode("TF");
        project.setName("TaskFlow Platform");
        project.setDescription("Dogfooding TaskFlow to build TaskFlow.");
        project.setLeadId(lead.getId());
        projectRepository.save(project);

        for (User u : all) {
            ProjectMember pm = new ProjectMember();
            pm.setProjectId(project.getId());
            pm.setUserId(u.getId());
            memberRepository.save(pm);
        }

        IssueType[] types = IssueType.values();
        Priority[] priorities = Priority.values();
        IssueStatus[] boardStatuses = {
                IssueStatus.TODO, IssueStatus.TODO, IssueStatus.IN_PROGRESS,
                IssueStatus.IN_REVIEW, IssueStatus.DONE, IssueStatus.BLOCKED};
        List<Skill> skillList = new ArrayList<>(skills.values());
        var rnd = ThreadLocalRandom.current();

        for (int n = 1; n <= 30; n++) {
            Issue issue = new Issue();
            issue.setProjectId(project.getId());
            issue.setIssueKey("TF-" + n);
            issue.setTitle("Seeded issue #" + n);
            issue.setDescription("Auto-generated demo issue " + n);
            issue.setType(types[rnd.nextInt(types.length)]);
            issue.setPriority(priorities[rnd.nextInt(priorities.length)]);
            issue.setStatus(boardStatuses[rnd.nextInt(boardStatuses.length)]);
            issue.setReporterId(lead.getId());
            issue.setBoardOrder(n);
            if (rnd.nextBoolean()) {
                issue.setAssigneeId(all.get(rnd.nextInt(all.size())).getId());
            }
            issueRepository.save(issue);

            Skill req = skillList.get(rnd.nextInt(skillList.size()));
            IssueRequiredSkill irs = new IssueRequiredSkill();
            irs.setIssueId(issue.getId());
            irs.setSkillId(req.getId());
            irs.setWeight(1 + rnd.nextInt(3));
            requiredSkillRepository.save(irs);
        }

        project.setIssueCounter(30);
        projectRepository.save(project);
        log.info("seed complete: {} users, 1 project, 30 issues", all.size());
    }

    private User user(String email, String name, Role role, int wip, String pwHash) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setWipLimit(wip);
        u.setPasswordHash(pwHash);
        return u;
    }

    private void addSkill(User u, Skill s, int proficiency) {
        UserSkill us = new UserSkill();
        us.setUserId(u.getId());
        us.setSkillId(s.getId());
        us.setProficiency(proficiency);
        userSkillRepository.save(us);
    }
}
