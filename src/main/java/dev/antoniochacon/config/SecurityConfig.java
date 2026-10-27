package dev.antoniochacon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/modelos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/dashboard", "/dashboard/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/reportes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/vehiculos/buscar").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "MECANICO")
                        .requestMatchers("/vehiculos/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/repuestos/compatibles").hasAnyRole("ADMINISTRADOR", "MECANICO")
                        .requestMatchers("/repuestos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/reparaciones/**").hasAnyRole("ADMINISTRADOR", "MECANICO")
                        .requestMatchers("/api/preferencias/**").authenticated()
                        .requestMatchers("/acceso-denegado").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/inicio", true)
                        .failureUrl("/login?error"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout"))
                .exceptionHandling(ex -> ex.accessDeniedHandler(accesoDenegado()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AccessDeniedHandler accesoDenegado() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            request.getRequestDispatcher("/acceso-denegado").forward(request, response);
        };
    }
}
