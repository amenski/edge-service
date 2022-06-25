package it.aman.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {
    private List<String> excludedUrls;
    private String tokenValidationUrl;
}