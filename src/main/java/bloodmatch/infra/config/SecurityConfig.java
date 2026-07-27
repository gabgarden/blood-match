package bloodmatch.infra.config;

import bloodmatch.infra.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("Username/password login is disabled");
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint((request, response, ex) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json");
              response.getWriter().write("{\"error\":\"Unauthorized\"}");
            })
            .accessDeniedHandler((request, response, ex) -> {
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.setContentType("application/json");
              response.getWriter().write("{\"error\":\"Forbidden\"}");
            }))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(OPTIONS, "/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
            .requestMatchers(POST, "/auth/login").permitAll()
            .requestMatchers(POST, "/parties/persons").permitAll()
            .requestMatchers(POST, "/parties/organizations").permitAll()

            .requestMatchers(GET, "/requests/recommendations").hasAnyAuthority("DONOR", "SYSTEM_ADMIN")
            .requestMatchers(GET, "/donation-requests/*").hasAnyAuthority("REQUESTER", "SYSTEM_ADMIN")
            .requestMatchers(POST, "/donation/create-pending").hasAnyAuthority("DONOR", "SYSTEM_ADMIN")
            .requestMatchers(POST, "/donation-requests").hasAnyAuthority("REQUESTER", "SYSTEM_ADMIN")
            .requestMatchers(DELETE, "/donation-requests/*").hasAnyAuthority("REQUESTER", "SYSTEM_ADMIN")
            .requestMatchers(PATCH, "/donation-requests/goal-blood-bags").hasAnyAuthority("REQUESTER", "SYSTEM_ADMIN")
            .requestMatchers(PATCH, "/donation-requests/date-limit").hasAnyAuthority("REQUESTER", "SYSTEM_ADMIN")
            .requestMatchers(POST, "/donations/completed").hasAnyAuthority("DONOR", "SYSTEM_ADMIN")
            .requestMatchers(PATCH, "/donations/complete").hasAnyAuthority("DONOR", "SYSTEM_ADMIN")
            .requestMatchers(POST, "/donations/create-pending").hasAnyAuthority("DONOR", "SYSTEM_ADMIN")
            .requestMatchers(PATCH, "/parties/name").authenticated()
            .requestMatchers(POST, "/donors").authenticated()
            .requestMatchers(PATCH, "/donors/profile").authenticated()
            .requestMatchers(PATCH, "/donors/recommendation-distance").hasAnyAuthority("DONOR", "SYSTEM_ADMIN")
            .requestMatchers(POST, "/requesters").authenticated()
            .requestMatchers(GET, "/donors/*/summary").authenticated()
            .requestMatchers(GET, "/donors/*/donations").authenticated()

            .anyRequest().authenticated())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(form -> form.disable())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
