package com.example.demo.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 加载 .env 文件配置
 * 在 Spring Boot 启动时自动加载项目根目录下的 .env 文件
 */
public class DotEnvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            // 从项目根目录加载 .env 文件
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            
            if (dotenv != null) {
                Map<String, Object> envMap = new HashMap<>();
                
                // 将 .env 文件中的变量添加到环境变量中
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    envMap.put(key, value);
                    // 同时设置到系统属性中，以便 @Value 注解可以读取
                    System.setProperty(key, value);
                    // 特别处理 DEEPSEEK_API_KEY，也设置 deepseek.api.key
                    if ("DEEPSEEK_API_KEY".equals(key)) {
                        System.setProperty("deepseek.api.key", value);
                        envMap.put("deepseek.api.key", value);
                    }
                });
                
                // 添加到 Spring Environment 中
                environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenv", envMap)
                );
                
                System.out.println("成功加载 .env 文件，共 " + envMap.size() + " 个配置项");
            }
        } catch (Exception e) {
            // .env 文件不存在或加载失败时，静默处理
            System.out.println("未找到 .env 文件，将使用系统环境变量或 application.properties 中的配置: " + e.getMessage());
        }
    }
}

