package com.pintu.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.dto.PassengerOrderCreateRequest;
import com.pintu.entity.OrderRecord;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void migrationsCreateSchemaSeedDemoDataAndRemainNonDestructive() {
        assertEquals(10, tableCount());
        assertEquals(50, completeDomainTableCount());
        assertEquals(3, count("user"));
        assertEquals(1, count("vehicle"));
        assertEquals(3, count("driver_trip"));
        assertEquals(Types.TIMESTAMP, columnType("passenger_order", "departure_time"));
        assertEquals(Types.TIMESTAMP, columnType("driver_trip", "departure_time"));
        assertEquals(Types.TIMESTAMP, columnType("order_record", "departure_time"));
        assertEquals(Types.DECIMAL, columnType("passenger_order", "start_lng"));
        assertEquals(Types.DECIMAL, columnType("driver_trip", "start_lng"));
        assertEquals(Types.TIMESTAMP, columnType("order_record", "actual_departure"));

        jdbcTemplate.update("""
                INSERT INTO passenger_order
                    (id, passenger_id, start_location, end_location, start_lng, start_lat,
                     end_lng, end_lat, departure_time, passenger_count, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "po-migration-test",
                "passenger-001",
                "同济科技园",
                "SOHO",
                new BigDecimal("121.5065000"),
                new BigDecimal("31.2821000"),
                new BigDecimal("121.4503000"),
                new BigDecimal("31.2284000"),
                LocalDateTime.of(2026, 6, 14, 18, 30),
                1,
                "待匹配");

        flyway.migrate();

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM passenger_order WHERE id = 'po-migration-test'",
                Integer.class));
        assertEquals(new BigDecimal("121.5065000"), jdbcTemplate.queryForObject(
                "SELECT start_lng FROM passenger_order WHERE id = 'po-migration-test'",
                BigDecimal.class));
        assertEquals(17, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17') AND success = TRUE",
                Integer.class));
        assertEquals(Types.VARCHAR, columnType("location_report", "client_report_id"));
        assertEquals(Types.INTEGER, columnType("location_report", "report_interval_seconds"));
        assertEquals(Types.INTEGER, columnType("user", "token_version"));
        String encryptedPhone = jdbcTemplate.queryForObject(
                "SELECT phone FROM `user` WHERE id = 'passenger-001'", String.class);
        org.junit.jupiter.api.Assertions.assertTrue(encryptedPhone.startsWith("gcm:"));
        org.junit.jupiter.api.Assertions.assertNotEquals("13800005678", encryptedPhone);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO vehicle (id, driver_id, plate, model, seats, status)
                VALUES ('veh-invalid-owner', 'missing-user', '沪A·TEST1', '测试车', 4, '待审核')
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO passenger_order
                    (id, passenger_id, start_location, end_location, departure_time, passenger_count, status)
                VALUES ('po-invalid-count', 'passenger-001', 'A', 'B', '2026-06-14 20:00', 0, '待匹配')
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO passenger_order
                    (id, passenger_id, start_location, end_location, departure_time, passenger_count, status)
                VALUES ('po-invalid-status', 'passenger-001', 'A', 'B', '2026-06-14 20:00', 1, '未知状态')
                """));

        jdbcTemplate.update("""
                INSERT INTO match_request
                    (id, passenger_order_id, driver_trip_id, passenger_id, driver_id, status)
                VALUES ('req-review-test', 'po-migration-test', 'trip-001',
                        'passenger-001', 'driver-001', '已同意')
                """);
        jdbcTemplate.update("""
                INSERT INTO order_record
                    (id, passenger_order_id, driver_trip_id, match_request_id, passenger_id, driver_id,
                     start_location, end_location, departure_time, passenger_count, price, status)
                VALUES ('order-review-test', 'po-migration-test', 'trip-001', 'req-review-test',
                        'passenger-001', 'driver-001', '同济科技园', 'SOHO',
                        '2026-06-14 18:30', 1, 18.60, '已完成')
                """);
        jdbcTemplate.update("""
                INSERT INTO review
                    (id, order_id, passenger_id, driver_id, reviewer_id, reviewee_id, rating)
                VALUES ('review-passenger-test', 'order-review-test', 'passenger-001', 'driver-001',
                        'passenger-001', 'driver-001', 5)
                """);
        jdbcTemplate.update("""
                INSERT INTO review
                    (id, order_id, passenger_id, driver_id, reviewer_id, reviewee_id, rating)
                VALUES ('review-driver-test', 'order-review-test', 'passenger-001', 'driver-001',
                        'driver-001', 'passenger-001', 5)
                """);
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review WHERE order_id = 'order-review-test'",
                Integer.class));
    }

    @Test
    void apiUsesShanghaiMinutePrecisionDateTimeContract() throws Exception {
        PassengerOrderCreateRequest request = objectMapper.readValue("""
                {
                  "start": "同济科技园",
                  "end": "SOHO",
                  "departureTime": "2026-06-14 18:30",
                  "passengerCount": 1
                }
                """, PassengerOrderCreateRequest.class);
        assertEquals(LocalDateTime.of(2026, 6, 14, 18, 30), request.getDepartureTime());

        OrderRecord order = new OrderRecord();
        order.setDepartureTime(request.getDepartureTime());
        assertEquals(
                "2026-06-14 18:30",
                objectMapper.readTree(objectMapper.writeValueAsString(order))
                        .get("departureTime")
                        .asText()
        );
    }

    private int tableCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'user', 'vehicle', 'passenger_order', 'driver_trip', 'match_request',
                    'order_record', 'payment', 'review', 'audit_record', 'credit_record'
                  )
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int completeDomainTableCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'user', 'vehicle', 'passenger_order', 'driver_trip', 'match_request',
                    'order_record', 'payment', 'review', 'audit_record', 'credit_record',
                    'order_status_history', 'auth_refresh_token', 'verification_code',
                    'password_reset_token', 'login_failure', 'identity_application',
                    'identity_attachment', 'vehicle_attachment', 'audit_history', 'conversation',
                    'chat_message', 'negotiation_proposal', 'proposal_confirmation', 'notification',
                    'credit_rule', 'blacklist', 'ban_record', 'appeal_ticket', 'appeal_transition',
                    'appeal_evidence', 'arbitration_record', 'rule_config', 'rule_version',
                    'rule_rollback', 'location_report', 'trajectory_point', 'safety_alert',
                    'emergency_contact', 'security_audit_event', 'user_role', 'user_address',
                    'admin_role_assignment', 'admin_role_permission', 'virtual_phone_binding',
                    'trip_share_token', 'account_preference', 'account_binding',
                    'support_session', 'support_message', 'data_retention_policy'
                  )
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Integer.class);
        return count == null ? 0 : count;
    }

    private int columnType(String table, String column) {
        return jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            try (var columns = connection.getMetaData().getColumns(null, null, table, column)) {
                if (!columns.next()) {
                    throw new IllegalStateException("Missing column " + table + "." + column);
                }
                return columns.getInt("DATA_TYPE");
            }
        });
    }
}
