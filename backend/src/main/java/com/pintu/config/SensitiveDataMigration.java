package com.pintu.config;

import com.pintu.security.SensitiveDataCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SensitiveDataMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final SensitiveDataCrypto crypto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username, phone, phone_hash FROM `user` WHERE phone IS NOT NULL");
        for (Map<String, Object> user : users) {
            String id = String.valueOf(user.get("id"));
            String username = String.valueOf(user.get("username"));
            String storedPhone = String.valueOf(user.get("phone"));
            String plaintext = crypto.decrypt(storedPhone);
            String internalUsername = username.equals(plaintext) ? "u_" + id : username;
            String encryptedPhone = crypto.isEncrypted(storedPhone)
                    ? storedPhone : crypto.encrypt(plaintext);
            jdbcTemplate.update("""
                    UPDATE `user`
                    SET phone = ?, phone_hash = ?, username = ?
                    WHERE id = ?
                    """, encryptedPhone, crypto.blindIndex(plaintext), internalUsername, id);
        }
    }
}
