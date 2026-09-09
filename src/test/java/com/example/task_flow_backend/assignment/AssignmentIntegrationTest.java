package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.auth.JwtService;
import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.issue.IssueStatuses;
import com.example.task_flow_backend.project.Project;
import com.example.task_flow_backend.project.ProjectMember;
import com.example.task_flow_backend.project.ProjectMemberRepository;
import com.example.task_flow_backend.project.ProjectRepository;
import com.example.task_flow_backend.skill.Skill;
import com.example.task_flow_backend.skill.SkillRepository;
import com.example.task_flow_backend.skill.UserSkill;
import com.example.task_flow_backend.skill.UserSkillRepository;
import com.example.task_flow_backend.support.AbstractIntegrationTest;
import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired UserSkillRepository userSkillRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository memberRepository;
    @Autowired IssueRepository issueRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private Project project;
    private User reporter;
    private User strong;
    private User weakA;
    private User weakB;
    private String reporterToken;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        memberRepository.deleteAll();
        userSkillRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        Skill java = skill("java");

        reporter = user("reporter@t.dev", Role.LEAD, 5);
        strong = user("strong@t.dev", Role.MEMBER, 4);
        weakA = user("weaka@t.dev", Role.MEMBER, 4);
        weakB = user("weakb@t.dev", Role.MEMBER, 4);

        proficiency(strong, java, 5);
        proficiency(weakA, java, 2);
        proficiency(weakB, java, 2);

        project = new Project();
        project.setKeyCode("TF");
        project.setName("Test");
        project.setLeadId(reporter.getId());
        projectRepository.save(project);
        for (User u : List.of(reporter, strong, weakA, weakB)) {
            member(project.getId(), u.getId());
        }

        reporterToken = jwtService.generateAccessToken(reporter);
    }

    @Test
    void newIssueIsAutoAssignedToTheBestSkillMatch() throws Exception {
        String key = createIssue("Wire up MySQL", java(java())).issueKey;

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Issue issue = issueRepository.findByIssueKey(key).orElseThrow();
            assertThat(issue.getAssigneeId()).isNotNull();
        });

        Issue issue = issueRepository.findByIssueKey(key).orElseThrow();
        assertThat(issue.getAssigneeId()).isEqualTo(strong.getId());
        assertThat(issue.getAssignmentMode().name()).isEqualTo("AUTO");
    }

    @Test
    void concurrentIssueCreationsNeverBreachAnyWipLimit() throws Exception {
        int count = 10;
        var pool = Executors.newFixedThreadPool(count);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(count);
        var failures = new AtomicInteger();

        for (int i = 0; i < count; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    createIssue("Concurrent", java(java()));
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        assertThat(failures.get()).isZero();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            long unassigned = issueRepository.findAll().stream()
                    .filter(i -> i.getAssigneeId() == null && i.getStatus() != IssueStatus.TRIAGE)
                    .count();
            assertThat(unassigned).isZero();
        });

        for (User u : List.of(strong, weakA, weakB)) {
            long open = issueRepository.countByAssigneeIdAndStatusIn(u.getId(), IssueStatuses.OPEN);
            assertThat(open)
                    .as("user %s open issues within WIP limit %s", u.getId(), u.getWipLimit())
                    .isLessThanOrEqualTo(u.getWipLimit());
        }
    }

    // ---------- helpers ----------

    private record CreatedIssue(String issueKey) {
    }

    private long java() {
        return skillRepository.findByNameIgnoreCase("java").orElseThrow().getId();
    }

    private static String java(long skillId) {
        return "[{\"skillId\":" + skillId + ",\"weight\":1}]";
    }

    private CreatedIssue createIssue(String title, String requiredSkillsJson) throws Exception {
        String body = """
                {"title":"%s","type":"TASK","priority":"MEDIUM","requiredSkills":%s}
                """.formatted(title, requiredSkillsJson);
        String response = mockMvc.perform(post("/api/v1/projects/{id}/issues", project.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String key = response.replaceAll(".*\"issueKey\":\"([^\"]+)\".*", "$1");
        return new CreatedIssue(key);
    }

    private Skill skill(String name) {
        Skill s = new Skill();
        s.setName(name);
        return skillRepository.save(s);
    }

    private User user(String email, Role role, int wip) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(email);
        u.setRole(role);
        u.setWipLimit(wip);
        u.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.save(u);
    }

    private void proficiency(User u, Skill s, int level) {
        UserSkill us = new UserSkill();
        us.setUserId(u.getId());
        us.setSkillId(s.getId());
        us.setProficiency(level);
        userSkillRepository.save(us);
    }

    private void member(Long projectId, Long userId) {
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(projectId);
        pm.setUserId(userId);
        memberRepository.save(pm);
    }
}
