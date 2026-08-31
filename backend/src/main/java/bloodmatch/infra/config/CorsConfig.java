package bloodmatch.infra.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,https://gabgarden.github.io}")
    private String allowedOrigins;

    private List<String> parsedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /**
     * Spring Security CORS filter uses this bean (WebMvcConfigurer alone is not enough
     * when {@code .cors(Customizer.withDefaults())} is enabled).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parsedOrigins());
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://bloodmatch.com.br",
                "http://bloodmatch.com.br:*",
                "https://bloodmatch.com.br",
                "https://bloodmatch.com.br:*",
                "http://www.bloodmatch.com.br",
                "http://www.bloodmatch.com.br:*",
                "https://www.bloodmatch.com.br",
                "https://www.bloodmatch.com.br:*",
                "http://179.*:*",
                "https://gabgarden.github.io"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(parsedOrigins().toArray(String[]::new))
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://bloodmatch.com.br",
                        "http://bloodmatch.com.br:*",
                        "https://bloodmatch.com.br",
                        "https://bloodmatch.com.br:*",
                        "http://www.bloodmatch.com.br",
                        "http://www.bloodmatch.com.br:*",
                        "https://www.bloodmatch.com.br",
                        "https://www.bloodmatch.com.br:*",
                        "http://179.*:*",
                        "https://gabgarden.github.io")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
