package com.pintu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pintuOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PinTu Private Carpooling API")
                        .description("PinTu 乘客端、司机端与管理端统一 HTTP API")
                        .version("0.2.0")
                        .license(new License().name("Course Project")));
    }
}
