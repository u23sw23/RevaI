package com.example.demo.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotEnvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            
            if (dotenv != null) {
                Map<String, Object> envMap = new HashMap<>();

                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    envMap.put(key, value);
                    
                    System.setProperty(key, value);
                    
                    if ("DEEPSEEK_API_KEY".equals(key)) {
                        System.setProperty("deepseek.api.key", value);
                        envMap.put("deepseek.api.key", value);
                    }
                });

                environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenv", envMap)
                );
            }
        } catch (Exception e) {
        }
    }
}

