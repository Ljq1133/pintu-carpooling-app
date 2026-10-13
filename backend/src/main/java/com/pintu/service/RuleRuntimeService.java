package com.pintu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.entity.RuleConfig;
import com.pintu.entity.RuleVersion;
import com.pintu.mapper.RuleConfigMapper;
import com.pintu.mapper.RuleVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RuleRuntimeService {

    private final RuleConfigMapper ruleConfigMapper;
    private final RuleVersionMapper ruleVersionMapper;
    private final ObjectMapper objectMapper;

    public BigDecimal decimalValue(String ruleType, String code, String field, BigDecimal fallback) {
        JsonNode node = activeContent(ruleType, code);
        if (node == null || !node.has(field) || !node.get(field).isNumber()) {
            return fallback;
        }
        return node.get(field).decimalValue();
    }

    public double doubleValue(String ruleType, String code, String field, double fallback) {
        JsonNode node = activeContent(ruleType, code);
        if (node == null || !node.has(field) || !node.get(field).isNumber()) {
            return fallback;
        }
        return node.get(field).asDouble();
    }

    public int intValue(String ruleType, String code, String field, int fallback) {
        JsonNode node = activeContent(ruleType, code);
        if (node == null || !node.has(field) || !node.get(field).isNumber()) {
            return fallback;
        }
        return node.get(field).asInt();
    }

    public long longValue(String ruleType, String code, String field, long fallback) {
        JsonNode node = activeContent(ruleType, code);
        if (node == null || !node.has(field) || !node.get(field).isNumber()) {
            return fallback;
        }
        return node.get(field).asLong();
    }

    private JsonNode activeContent(String ruleType, String code) {
        RuleConfig config = ruleConfigMapper.selectOne(new QueryWrapper<RuleConfig>()
                .eq("rule_type", ruleType)
                .eq("code", code)
                .eq("status", "ACTIVE")
                .isNotNull("active_version")
                .last("limit 1"));
        if (config == null) {
            return null;
        }
        RuleVersion version = ruleVersionMapper.selectOne(new QueryWrapper<RuleVersion>()
                .eq("rule_id", config.getId())
                .eq("version", config.getActiveVersion())
                .last("limit 1"));
        if (version == null || version.getContent() == null) {
            return null;
        }
        try {
            return objectMapper.readTree(version.getContent());
        } catch (Exception ignored) {
            return null;
        }
    }
}
