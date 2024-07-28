package com.mcp.crispy.common.config;

import com.mcp.crispy.auth.service.AuthenticationService;
import com.mcp.crispy.common.CustomAuthenticationEntryPoint;
import com.mcp.crispy.common.CustomLogoutSuccessHandler;
import com.mcp.crispy.common.filter.JwtAuthorizationFilter;
import com.mcp.crispy.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthenticationConfiguration authenticationConfiguration;
	private final JwtAuthorizationFilter jwtAuthorizationFilter;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(16, 32, 4, 512, 5);
	}


	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, EmployeeService employeeService, AuthenticationService authenticationService) throws Exception {
		http
				.cors(AbstractHttpConfigurer::disable)
				.csrf(AbstractHttpConfigurer::disable) // _csrf
				.headers(headersConfigurer -> headersConfigurer
						.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
				.authorizeHttpRequests(config -> config
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/api/email/verificationCode/verify/v1").permitAll()
						.requestMatchers("/api/employee/verify/email/v1").permitAll()
						.requestMatchers("/app/**", "/user/**", "/topic/**", "/error/**").permitAll()
						.requestMatchers("/css/**", "/js/**", "/img/**", "/resources/**",
								"/profiles/**", "/upload/**", "/franchise/**", "/crispy_img/**").permitAll()  // 인증 없이 접근 가능
						.requestMatchers("/", "/crispy", "/crispy/", "/CRISPY", "/CRISPY/").permitAll()
						.requestMatchers("/","/crispy/login", "/crispy/logout", "/crispy/signup",
								"/crispy/employee/findEmpId","/crispy/employee/findEmpId/result",
								"/crispy/employee/findEmpPw", "/crispy/employee/changeEmpPw").permitAll()
						.anyRequest().authenticated()) // 2024.06.02 JWT 사용으로 인한 인증 요청으로 변경
				.formLogin(login -> login
						.loginPage("/crispy/login"))
				.logout(logout -> logout
						.logoutUrl("/crispy/logout")
						.logoutSuccessHandler(new CustomLogoutSuccessHandler(employeeService, authenticationService)))
				.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(customAuthenticationEntryPoint));
		http.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}