package com.valenlabs.iopms;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end flow over a real HTTP server and an in-memory H2 database:
 * admin creates an intern, intern works, submits, and the admin reviews everything.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppFlowTest {

    @Autowired
    TestRestTemplate http;

    static String adminToken;
    static String internToken;
    static Long internId;
    static Long taskId;
    static Long projectId;
    static Long submissionId;

    private ResponseEntity<Map> call(String method, String url, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(url, HttpMethod.valueOf(method), new HttpEntity<>(body, headers), Map.class);
    }

    private String login(String email, String password) {
        ResponseEntity<Map> res = call("POST", "/api/auth/login", null, Map.of("email", email, "password", password));
        assertEquals(200, res.getStatusCode().value(), "login should succeed: " + res.getBody());
        return (String) res.getBody().get("token");
    }

    @Test
    @Order(1)
    void adminCanLogInWithSeedAccount() {
        adminToken = login("admin@valenlabs.com", "Admin@123");
    }

    @Test
    @Order(2)
    void adminCreatesIntern() {
        ResponseEntity<Map> res = call("POST", "/api/admin/interns", adminToken, Map.of(
                "name", "Ayush Kumar Sahu", "email", "ayush@valenlabs.com",
                "password", "Intern@123", "college", "VIT", "joinedDate", "2026-01-05"));
        assertEquals(200, res.getStatusCode().value(), res.getBody() + "");
        internId = ((Number) res.getBody().get("id")).longValue();
    }

    @Test
    @Order(3)
    void internCannotReachAdminEndpoints() {
        internToken = login("ayush@valenlabs.com", "Intern@123");
        assertEquals(403, call("GET", "/api/admin/interns", internToken, null).getStatusCode().value());
        assertEquals(401, call("GET", "/api/admin/interns", null, null).getStatusCode().value());
    }

    @Test
    @Order(4)
    void attendanceRulesHold() {
        assertEquals(200, call("POST", "/api/intern/attendance/start", internToken, null).getStatusCode().value());
        // second simultaneous start is rejected
        assertEquals(400, call("POST", "/api/intern/attendance/start", internToken, null).getStatusCode().value());
        ResponseEntity<Map> end = call("POST", "/api/intern/attendance/end", internToken, null);
        assertEquals(200, end.getStatusCode().value(), end.getBody() + "");
        assertTrue(end.getBody().containsKey("minutes"), "backend must calculate hours");
        // nothing left to end
        assertEquals(400, call("POST", "/api/intern/attendance/end", internToken, null).getStatusCode().value());
    }

    @Test
    @Order(5)
    void adminCorrectsAttendanceWithAudit() {
        // fetch the record through the admin per-intern endpoint (session was created today)
        String month = LocalDate.now().toString().substring(0, 7);
        ResponseEntity<Map[]> adminHistory = http.exchange(
                "/api/admin/attendance/intern/" + internId + "?month=" + month,
                HttpMethod.GET, new HttpEntity<>(auth(adminToken)), Map[].class);
        assertEquals(200, adminHistory.getStatusCode().value());
        long attId = ((Number) adminHistory.getBody()[0].get("id")).longValue();

        ResponseEntity<Map> res = call("POST", "/api/admin/attendance/" + attId + "/correct", adminToken, Map.of(
                "checkIn", LocalDate.now().atTime(9, 0).toString(),
                "checkOut", LocalDate.now().atTime(17, 0).toString(), "reason", "system outage"));
        assertEquals(200, res.getStatusCode().value(), res.getBody() + "");
        assertEquals(480, ((Number) res.getBody().get("minutes")).longValue());

        ResponseEntity<String> audit = http.exchange("/api/admin/attendance/" + attId + "/audit",
                HttpMethod.GET, new HttpEntity<>(auth(adminToken)), String.class);
        assertEquals(200, audit.getStatusCode().value());
        assertTrue(audit.getBody().contains("system outage"), "audit must record the reason");
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    @Test
    @Order(6)
    void taskLifecycle() {
        ResponseEntity<Map> created = call("POST", "/api/admin/tasks", adminToken, Map.of(
                "title", "Build IOPMS MVP", "description", "All modules", "assigneeId", internId,
                "priority", "HIGH", "startDate", "2026-01-05", "deadline", "2026-01-20"));
        assertEquals(200, created.getStatusCode().value(), created.getBody() + "");
        taskId = ((Number) created.getBody().get("id")).longValue();

        // intern moves to review
        ResponseEntity<Map> progress = call("PUT", "/api/intern/tasks/" + taskId + "/progress", internToken,
                Map.of("progress", 80, "status", "UNDER_REVIEW"));
        assertEquals(200, progress.getStatusCode().value(), progress.getBody() + "");
        assertEquals("UNDER_REVIEW", progress.getBody().get("status"));

        // intern cannot close a task or touch someone else's
        assertEquals(400, call("PUT", "/api/intern/tasks/" + taskId + "/progress", internToken,
                Map.of("progress", 100, "status", "COMPLETED")).getStatusCode().value());
    }

    @Test
    @Order(7)
    void dailyReportOnePerDayAndReview() {
        String today = LocalDate.now().toString();
        ResponseEntity<Map> res = call("POST", "/api/intern/reports", internToken, Map.of(
                "date", today, "tasksCompleted", "Modules A-B", "workPerformed", "Coding",
                "timeSpent", "A: 3h, B: 4h", "progress", 60, "challenges", "JWT expiry",
                "pendingWork", "Dashboard", "nextDayPlan", "Dashboard", "links", "https://github.com/x/y"));
        assertEquals(200, res.getStatusCode().value(), res.getBody() + "");
        long reportId = ((Number) res.getBody().get("id")).longValue();

        // duplicate date rejected
        assertEquals(409, call("POST", "/api/intern/reports", internToken, Map.of(
                "date", today, "tasksCompleted", "x", "workPerformed", "y", "timeSpent", "z",
                "progress", 10, "pendingWork", "p", "nextDayPlan", "n")).getStatusCode().value());

        assertEquals(200, call("POST", "/api/admin/reports/" + reportId + "/review", adminToken,
                Map.of("status", "APPROVED", "remarks", "good")).getStatusCode().value());
        // PENDING is not a review outcome
        assertEquals(400, call("POST", "/api/admin/reports/" + reportId + "/review", adminToken,
                Map.of("status", "PENDING")).getStatusCode().value());
    }

    @Test
    @Order(8)
    void projectSubmissionWithVersions() {
        ResponseEntity<Map> project = call("POST", "/api/admin/projects", adminToken, Map.of(
                "title", "IOPMS", "description", "MVP", "supervisor", "Valen Labs",
                "startDate", "2026-01-05", "expectedEndDate", "2026-03-01",
                "internIds", java.util.List.of(internId)));
        assertEquals(200, project.getStatusCode().value(), project.getBody() + "");
        projectId = ((Number) project.getBody().get("id")).longValue();

        // listing must serialize the lazy interns collection inside a session
        ResponseEntity<Map[]> adminList = http.exchange("/api/admin/projects",
                HttpMethod.GET, new HttpEntity<>(auth(adminToken)), Map[].class);
        assertEquals(200, adminList.getStatusCode().value());
        assertTrue(java.util.Arrays.stream(adminList.getBody())
                .anyMatch(p -> ((Number) p.get("id")).longValue() == projectId));
        ResponseEntity<Map[]> internList = http.exchange("/api/intern/projects",
                HttpMethod.GET, new HttpEntity<>(auth(internToken)), Map[].class);
        assertEquals(200, internList.getStatusCode().value());
        assertEquals(1, internList.getBody().length, "assigned intern sees exactly their project");

        ResponseEntity<Map> s1 = call("POST", "/api/intern/projects/" + projectId + "/submissions",
                internToken, Map.of("repoUrl", "https://github.com/ayush/iopms", "notes", "first cut"));
        assertEquals(200, s1.getStatusCode().value(), s1.getBody() + "");
        submissionId = ((Number) s1.getBody().get("id")).longValue();
        assertEquals(1, ((Number) s1.getBody().get("version")).longValue());

        ResponseEntity<Map> s2 = call("POST", "/api/intern/projects/" + projectId + "/submissions",
                internToken, Map.of("repoUrl", "https://github.com/ayush/iopms", "notes", "review fixes"));
        assertEquals(2, ((Number) s2.getBody().get("version")).longValue(), "version history increments");

        assertEquals(200, call("POST", "/api/admin/submissions/" + submissionId + "/review", adminToken,
                Map.of("status", "CHANGES_REQUESTED", "remarks", "add swagger")).getStatusCode().value());
    }

    @Test
    @Order(9)
    void dashboardAndConsolidatedReport() {
        String month = LocalDate.now().toString().substring(0, 7);
        ResponseEntity<Map> dash = call("GET", "/api/admin/dashboard", adminToken, null);
        assertEquals(200, dash.getStatusCode().value(), dash.getBody() + "");
        Map team = (Map) dash.getBody().get("team");
        assertEquals(1, ((Number) team.get("interns")).longValue());

        ResponseEntity<Map> report = call("GET", "/api/admin/interns/" + internId + "/report?month=" + month,
                adminToken, null);
        assertEquals(200, report.getStatusCode().value(), report.getBody() + "");
        assertEquals(480L, ((Number) report.getBody().get("totalMinutes")).longValue());

        ResponseEntity<String> csv = http.exchange("/api/admin/interns/" + internId + "/report.csv?month=" + month,
                HttpMethod.GET, new HttpEntity<>(auth(adminToken)), String.class);
        assertEquals(200, csv.getStatusCode().value());
        assertTrue(csv.getBody().contains("ATTENDANCE"), "CSV must contain attendance section");
    }
}
