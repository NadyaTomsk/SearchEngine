package main.controller;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "configs")
public class ConfigurationApplication {
    public String userAgent;
    public String webInterfacePath;
    public List<Site> sites;

    @Data
    public static class Site {
        @Getter
        @Setter
        private String url;
        @Setter
        @Getter
        private String name;
    }
}
