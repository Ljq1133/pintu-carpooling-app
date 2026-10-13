package com.pintu.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedApiRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/passenger/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void roleBasedAccessAllowsOwnerDomainAndRejectsAdminDomain() throws Exception {
        JsonNode login = login("passenger_demo", "passenger", "123456");
        String accessToken = login.path("data").path("token").asText();
        mockMvc.perform(get("/api/passenger/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("passenger-001"));
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void adminMeExposesSuperAdminPermissionsAndRejectRequiresReason() throws Exception {
        String adminToken = login("admin_demo", "admin", "123456")
                .path("data").path("token").asText();

        mockMvc.perform(get("/api/admin/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subRoles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.permissions[0].code").isNotEmpty());

        mockMvc.perform(post("/api/admin/audits/users/audit-user-001/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", " "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void refreshTokenIsRotatedAndCannotBeReused() throws Exception {
        String refreshToken = login("driver_demo", "driver", "123456")
                .path("data").path("refreshToken").asText();
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void registrationRequiresSingleUseCodeAndInitializesPassenger() throws Exception {
        String phone = "13900001234";
        String code = sendCode(phone, "REGISTER");
        Map<String, String> request = Map.of(
                "phone", phone,
                "password", "abc12345",
                "name", "New passenger",
                "code", code
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("passenger"))
                .andExpect(jsonPath("$.data.user.credit").value(80))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void codeLoginAndPasswordResetRevokeAllOldTokens() throws Exception {
        String phone = "13900005678";
        JsonNode registration = register(phone, "abc12345", "Lifecycle user");
        String oldAccessToken = registration.path("data").path("token").asText();
        String oldRefreshToken = registration.path("data").path("refreshToken").asText();

        String loginCode = sendCode(phone, "LOGIN");
        mockMvc.perform(post("/api/auth/login/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone, "code", loginCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
        mockMvc.perform(post("/api/auth/login/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone, "code", loginCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1013));

        String resetCode = sendCode(phone, "RESET_PASSWORD");
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "phone", phone,
                                "code", resetCode,
                                "newPassword", "newPass123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/passenger/profile")
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", oldRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
        login(phone, "passenger", "newPass123");
    }

    @Test
    void verificationCodeSendIsRateLimited() throws Exception {
        String phone = "13900006789";
        sendCode(phone, "REGISTER");
        mockMvc.perform(post("/api/auth/codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone, "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1012));
    }

    @Test
    void repeatedPasswordFailuresTemporarilyLockAccount() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "username", "locked-test-user",
                                    "password", "wrong-password",
                                    "role", "passenger"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1003));
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "locked-test-user",
                                "password", "123456",
                                "role", "passenger"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1011));
    }

    @Test
    void driverRoleRequiresIdentityApprovalAndExplicitSwitch() throws Exception {
        String phone = "13900008901";
        String passengerToken = register(phone, "abc12345", "Driver applicant")
                .path("data").path("token").asText();

        JsonNode application = performJson(
                post("/api/account/driver-identity")
                        .header("Authorization", "Bearer " + passengerToken),
                Map.of(
                        "realName", "Test Driver",
                        "idCard", "110101199001011234",
                        "idCardFrontKey", "identity/front/test.jpg",
                        "idCardBackKey", "identity/back/test.jpg",
                        "livenessResult", "PASS"
                )
        );
        String applicationId = application.path("data").path("id").asText();

        mockMvc.perform(post("/api/account/role")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "driver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
        mockMvc.perform(get("/api/driver/profile")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin_demo", "admin", "123456")
                .path("data").path("token").asText();
        JsonNode audits = objectMapper.readTree(mockMvc.perform(get("/api/admin/audits/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String auditId = null;
        for (JsonNode audit : audits.path("data")) {
            if (applicationId.equals(audit.path("targetId").asText())) {
                auditId = audit.path("id").asText();
                break;
            }
        }
        if (auditId == null) {
            throw new AssertionError("Identity audit was not created: " + audits);
        }
        mockMvc.perform(post("/api/admin/audits/users/" + auditId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        JsonNode switched = performJson(
                post("/api/account/role")
                        .header("Authorization", "Bearer " + passengerToken),
                Map.of("role", "driver")
        );
        String driverToken = switched.path("data").path("token").asText();
        mockMvc.perform(get("/api/driver/profile")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void authenticatedUserCanChangePasswordAndPhone() throws Exception {
        String phone = "13900007890";
        String accessToken = register(phone, "abc12345", "Settings user")
                .path("data").path("token").asText();
        mockMvc.perform(put("/api/account/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "oldPassword", "abc12345",
                                "newPassword", "changed123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String newAccessToken = login(phone, "passenger", "changed123")
                .path("data").path("token").asText();
        String newPhone = "13900007891";
        mockMvc.perform(put("/api/account/phone")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "newPhone", newPhone,
                                "code", sendCode(newPhone, "CHANGE_PHONE")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        login(newPhone, "passenger", "changed123");
    }

    @Test
    void userCanEditProfileAndDriverVehicleCrudResetsAuditStatus() throws Exception {
        String passengerToken = login("passenger_demo", "passenger", "123456")
                .path("data").path("token").asText();
        mockMvc.perform(put("/api/account/profile")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Updated Passenger",
                                "avatarUrl", "https://example.test/avatar.png",
                                "gender", "undisclosed"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Passenger"))
                .andExpect(jsonPath("$.data.gender").value("undisclosed"));

        String driverToken = login("driver_demo", "driver", "123456")
                .path("data").path("token").asText();
        JsonNode created = performJson(
                post("/api/driver/vehicles")
                        .header("Authorization", "Bearer " + driverToken),
                Map.of(
                        "plate", "SH-A12345",
                        "brand", "Test Brand",
                        "model", "Test Model",
                        "color", "White",
                        "seats", 4,
                        "vehicleType", "sedan",
                        "licenseKey", "vehicle/license/test.jpg"
                )
        );
        String vehicleId = created.path("data").path("id").asText();
        org.junit.jupiter.api.Assertions.assertFalse(vehicleId.isBlank());

        mockMvc.perform(get("/api/driver/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brand").value("Test Brand"));

        mockMvc.perform(put("/api/driver/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "plate", "SH-A12345",
                                "brand", "Test Brand",
                                "model", "Updated Model",
                                "color", "Black",
                                "seats", 4,
                                "vehicleType", "sedan",
                                "licenseKey", "vehicle/license/test-v2.jpg"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model").value("Updated Model"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/driver/vehicles/" + vehicleId)
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private JsonNode register(String phone, String password, String name) throws Exception {
        return performJson(post("/api/auth/register"), Map.of(
                "phone", phone,
                "password", password,
                "name", name,
                "code", sendCode(phone, "REGISTER")
        ));
    }

    private JsonNode login(String username, String role, String password) throws Exception {
        JsonNode response = performJson(post("/api/auth/login"), Map.of(
                "username", username,
                "password", password,
                "role", role
        ));
        if (response.path("data").path("token").asText().isBlank()) {
            throw new AssertionError("Login failed: " + response);
        }
        return response;
    }

    private String sendCode(String phone, String purpose) throws Exception {
        JsonNode response = performJson(post("/api/auth/codes"), Map.of(
                "phone", phone,
                "purpose", purpose
        ));
        String code = response.path("data").path("debugCode").asText();
        if (code.isBlank()) {
            throw new AssertionError("Test profile did not expose mock code: " + response);
        }
        return code;
    }

    private JsonNode performJson(MockHttpServletRequestBuilder request, Object body) throws Exception {
        String response = mockMvc.perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
