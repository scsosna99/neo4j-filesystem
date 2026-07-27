/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@SecurityScheme(name = "basicAuth", scheme = "basic", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class SecurityConfig {

    /**
     * Constructor
     * @param userStore data mapping users to groups
     * @return Spring filter implementing Spring security
     */
    @Bean
    public BasicAuthFilter demoBasicAuthFilter(final UserGroupStore userStore) {
        return new BasicAuthFilter(userStore);
    }

    /**
     * Adds http filter for applying/skipping security checks, standard Spring filter stuff
     * @param http
     * @param demoFilter
     * @return
     * @throws Exception thrown when anything bad occurs
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http, final BasicAuthFilter demoFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Allow swagger and docs
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger/**"
                ).permitAll()
                // Require auth for APIs
                .requestMatchers("/neo4jfs/api/**").authenticated()
                // everything else rejected
                .anyRequest().not().permitAll()
            )
            .addFilterBefore(demoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
