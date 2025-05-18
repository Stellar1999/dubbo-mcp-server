package org.apache.dubbo.mcp.server.demo;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class McpDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpDemoApplication.class, args);
    }
}
