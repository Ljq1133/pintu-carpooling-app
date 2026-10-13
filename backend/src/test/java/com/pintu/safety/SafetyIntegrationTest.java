package com.pintu.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.entity.OrderRecord;
import com.pintu.enums.OrderStatus;
import com.pintu.mapper.OrderRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
class SafetyIntegrationTest {

    private static final String PASSENGER_ORDER_ID = "po-safety-api";
    private static final String MATCH_REQUEST_ID = "req-safety-api";
    private static final String ORDER_ID = "order-safety-api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRecordMapper orderRecordMapper;

    @BeforeEach
    void createOngoingOrder() {
        jdbcTemplate.update("""
                INSERT INTO passenger_order
                    (id, passenger_id, start_location, end_location, start_lng, start_lat,
                     end_lng, end_lat, departure_time, passenger_count, status)
                VALUES (?, 'passenger-001', 'Safety Start', 'Safety End', ?, ?, ?, ?,
                        '2026-06-15 08:30', 1, ?)
                """,
                PASSENGER_ORDER_ID,
                new BigDecimal("121.5000000"),
                new BigDecimal("31.2000000"),
                new BigDecimal("121.6000000"),
                new BigDecimal("31.3000000"),
                OrderStatus.ONGOING.getValue());
        jdbcTemplate.update("""
                INSERT INTO match_request
                    (id, passenger_order_id, driver_trip_id, passenger_id, driver_id, status)
                VALUES (?, ?, 'trip-001', 'passenger-001', 'driver-001', '已同意')
                """,
                MATCH_REQUEST_ID,
                PASSENGER_ORDER_ID);
        jdbcTemplate.update("""
                INSERT INTO order_record
                    (id, passenger_order_id, driver_trip_id, match_request_id, passenger_id, driver_id,
                     start_location, end_location, start_lng, start_lat, end_lng, end_lat,
                     departure_time, passenger_count, price, status)
                VALUES (?, ?, 'trip-001', ?, 'passenger-001', 'driver-001',
                        'Safety Start', 'Safety End', ?, ?, ?, ?, '2026-06-15 08:30', 1, 18.60, ?)
                """,
                ORDER_ID,
                PASSENGER_ORDER_ID,
                MATCH_REQUEST_ID,
                new BigDecimal("121.5000000"),
                new BigDecimal("31.2000000"),
                new BigDecimal("121.6000000"),
                new BigDecimal("31.3000000"),
                OrderStatus.ONGOING.getValue());
    }

    @Test
    void contactApiEncryptsPhoneScopesOwnershipAndKeepsOnlyOnePrimary() throws Exception {
        String passengerToken = token("passenger_demo", "passenger");
        String driverToken = token("driver_demo", "driver");

        JsonNode first = performJson(
                post("/api/safety/emergency-contacts").header("Authorization", bearer(passengerToken)),
                contact("Passenger Family", "13812345678", "Family", true)
        );
        String firstId = first.path("data").path("id").asText();

        assertEquals("138****5678", first.path("data").path("phoneMasked").asText());
        assertNotEquals("13812345678", first.toString());
        String storedCipher = jdbcTemplate.queryForObject(
                "SELECT phone_cipher FROM emergency_contact WHERE id = ?",
                String.class,
                firstId
        );
        assertTrue(storedCipher.startsWith("gcm:"));
        assertNotEquals("13812345678", storedCipher);

        mockMvc.perform(get("/api/safety/emergency-contacts")
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        performJson(
                put("/api/safety/emergency-contacts/" + firstId)
                        .header("Authorization", bearer(passengerToken)),
                contact("Passenger Family", "", "Emergency Family", true)
        );
        assertEquals(storedCipher, jdbcTemplate.queryForObject(
                "SELECT phone_cipher FROM emergency_contact WHERE id = ?",
                String.class,
                firstId
        ));

        JsonNode second = performJson(
                post("/api/safety/emergency-contacts").header("Authorization", bearer(passengerToken)),
                contact("Passenger Friend", "13912345678", "Friend", true)
        );
        String secondId = second.path("data").path("id").asText();
        assertEquals(0, contactPrimary(firstId));
        assertEquals(1, contactPrimary(secondId));

        mockMvc.perform(delete("/api/safety/emergency-contacts/" + firstId)
                        .header("Authorization", bearer(driverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1404));
        assertEquals(1, count("SELECT COUNT(*) FROM emergency_contact WHERE id = ?", firstId));

        mockMvc.perform(delete("/api/safety/emergency-contacts/" + firstId)
                        .header("Authorization", bearer(passengerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(0, count("SELECT COUNT(*) FROM emergency_contact WHERE id = ?", firstId));
    }

    @Test
    void onlyOrderParticipantsCanAlertAndAlertDoesNotMutateOrder() throws Exception {
        String passengerToken = token("passenger_demo", "passenger");
        String driverToken = token("driver_demo", "driver");
        String adminToken = token("admin_demo", "admin");

        JsonNode passengerAlert = performJson(
                post("/api/safety/orders/" + ORDER_ID + "/alerts")
                        .header("Authorization", bearer(passengerToken)),
                Map.of(
                        "alertType", "ONE_CLICK",
                        "longitude", new BigDecimal("121.5000000"),
                        "latitude", new BigDecimal("31.2000000"),
                        "details", "Passenger integration test"
                )
        );
        assertEquals("passenger-001", alertUser(passengerAlert.path("data").path("id").asText()));
        assertTrue(passengerAlert.path("data").path("policeReportId").asText().startsWith("police-"));

        JsonNode driverAlert = performJson(
                post("/api/safety/orders/" + ORDER_ID + "/alerts")
                        .header("Authorization", bearer(driverToken)),
                Map.of("alertType", "ONE_CLICK", "details", "Driver integration test")
        );
        assertEquals("driver-001", alertUser(driverAlert.path("data").path("id").asText()));
        assertEquals(2, count("SELECT COUNT(*) FROM safety_alert WHERE order_id = ?", ORDER_ID));
        assertEquals(OrderStatus.ONGOING.getValue(), orderStatus());

        mockMvc.perform(post("/api/safety/orders/" + ORDER_ID + "/alerts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alertType", "ONE_CLICK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
        assertEquals(2, count("SELECT COUNT(*) FROM safety_alert WHERE order_id = ?", ORDER_ID));

        OrderRecord order = orderRecordMapper.selectById(ORDER_ID);
        order.setStatus(OrderStatus.CONFIRMED.getValue());
        orderRecordMapper.updateById(order);
        mockMvc.perform(post("/api/safety/orders/" + ORDER_ID + "/alerts")
                        .header("Authorization", bearer(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alertType", "ONE_CLICK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1014));
        assertEquals(2, count("SELECT COUNT(*) FROM safety_alert WHERE order_id = ?", ORDER_ID));
    }

    @Test
    void safetyApisRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/safety/emergency-contacts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
        mockMvc.perform(post("/api/safety/orders/" + ORDER_ID + "/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alertType", "ONE_CLICK"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    private Map<String, Object> contact(
            String name,
            String phone,
            String relationship,
            boolean primary) {
        return Map.of(
                "name", name,
                "phone", phone,
                "relationship", relationship,
                "isPrimary", primary
        );
    }

    private String token(String username, String role) throws Exception {
        return performJson(post("/api/auth/login"), Map.of(
                "username", username,
                "password", "123456",
                "role", role
        )).path("data").path("token").asText();
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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private int contactPrimary(String contactId) {
        return jdbcTemplate.queryForObject(
                "SELECT is_primary FROM emergency_contact WHERE id = ?",
                Integer.class,
                contactId
        );
    }

    private String alertUser(String alertId) {
        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM safety_alert WHERE id = ?",
                String.class,
                alertId
        );
    }

    private String orderStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM order_record WHERE id = ?",
                String.class,
                ORDER_ID
        );
    }

    private int count(String sql, String id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }
}
