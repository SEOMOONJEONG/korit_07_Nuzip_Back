package com.highlight.nuzip.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * Swagger UI에 JWT 인증을 위한 'Authorize' 버튼과 스키마를 추가하는 설정
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Bearer Token 인증 방식을 정의합니다.
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP) // 인증 타입: HTTP
                                .scheme("bearer")               // 스키마: Bearer
                                .bearerFormat("JWT")            // 포맷: JWT
                                .name("Authorization")          // 헤더 이름
                                .description("JWT 인증 토큰을 입력하세요. (예: Bearer eyJ... )")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));

    }
}