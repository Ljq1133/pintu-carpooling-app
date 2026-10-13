package com.pintu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用入口。
 * 由成员 A 维护，负责启动后端服务并扫描 MyBatis Mapper。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.pintu.mapper")
public class PintuApplication {

    public static void main(String[] args) {
        SpringApplication.run(PintuApplication.class, args);
    }
}
