package com.appointech.notificaciones;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfig {

    private final JwtFiltro jwtFiltro;

    public SeguridadConfig(JwtFiltro jwtFiltro) {
        this.jwtFiltro = jwtFiltro;
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.GET, "/api/notificaciones/salud").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
