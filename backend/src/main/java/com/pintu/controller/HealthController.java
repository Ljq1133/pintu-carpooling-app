package com.pintu.controller;

import com.pintu.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口。
 * 用于联调握手、CORS 校验和部署巡检。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("alive");
    }
}
