package com.yaldi.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${springdoc.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription("API Server");

        Info info = new Info()
                .title("YALDI API Documentation")
                .version("1.0.0")
                .description("YALDI 프로젝트 API 문서입니다.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
