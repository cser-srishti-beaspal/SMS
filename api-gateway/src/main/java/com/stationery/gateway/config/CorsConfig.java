package com.stationery.gateway.config;
// package declaration

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS Configuration for the API Gateway.
 * <p>
 * Configures Cross-Origin Resource Sharing (CORS) to allow all origins,
 * methods, and headers during development. This should be restricted
 * to specific origins in a production environment.
 * </p>
 */

//this tells our spring boot that this class contains setup and config settings.
@Configuration
public class CorsConfig {

    // tells spring to run this method at startup, take the object it returns and keep it in your main memory.
    @Bean

    //corswebfilter - return data type.
    // acts as a filter to incoming http request to check cors rules
    public CorsWebFilter corsWebFilter() {

        //holds the rules of the cors, and creates a fresh, empty configuration object in memory.
        CorsConfiguration corsConfig = new CorsConfiguration();

        // * allows all origins. In production, specify allowed origins for security.
        corsConfig.setAllowedOriginPatterns(List.of("*"));

        // Allow common HTTP methods. Adjust as necessary for your application.
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        //tells server to accept requests containing any custom http header sent by frontend.
        corsConfig.setAllowedHeaders(List.of("*"));
        //allowed fronend to see the Authorization and Content-Type headers in the response.
        corsConfig.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // tells the browser that it is allowed to send sensitive credentials
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        // Creates an instance of a reactive CORS configuration source. It is responsible for mapping our defined  corsConfig  rules to specific URL paths.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}