package com.mcp.crispy.auth.service;

import com.mcp.crispy.admin.dto.AdminDto;
import com.mcp.crispy.admin.service.AdminService;
import com.mcp.crispy.auth.domain.AdminPrincipal;
import com.mcp.crispy.auth.domain.EmployeePrincipal;
import com.mcp.crispy.common.config.CrispyUserDetailsService;
import com.mcp.crispy.common.exception.InvalidLoginRequestException;
import com.mcp.crispy.common.utils.JwtUtil;
import com.mcp.crispy.employee.dto.EmployeeDto;
import com.mcp.crispy.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final CrispyUserDetailsService crispyUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmployeeService employeeService;
    private final AdminService adminService;

    // 로그인시 토큰 부여
    public UserDetails login(String username, String password) {
        validateLoginRequest(username, password);
        UserDetails userDetails = crispyUserDetailsService.loadUserByUsername(username);

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new IllegalArgumentException("비밀번호를 확인해주세요.");
        }

        if (userDetails instanceof EmployeePrincipal employeePrincipal) {
            EmployeeDto employee = employeePrincipal.getEmployee();
            String accessToken = jwtUtil.createAccessToken(employeePrincipal);
            String refreshToken = jwtUtil.createRefreshToken(employeePrincipal);
            employee.setAccessToken(accessToken);
            employeeService.updateRefreshToken(refreshToken, employee.getEmpNo());
            employee.setRefreshToken(refreshToken);
            return employeePrincipal;
        } else if (userDetails instanceof AdminPrincipal adminPrincipal) {
            AdminDto admin = adminPrincipal.getAdmin();
            String accessToken = jwtUtil.createAccessToken(adminPrincipal);
            String refreshToken = jwtUtil.createRefreshToken(adminPrincipal);
            adminService.updateRefreshToken(refreshToken, admin.getAdminNo());
            admin.setAccessToken(accessToken);
            admin.setRefreshToken(refreshToken);
            return adminPrincipal;
        }

        return userDetails;
    }

    // 로그아웃 시 리프레시 토큰 삭제
    public void logout(String username) {
        UserDetails userDetails = crispyUserDetailsService.loadUserByUsername(username);

        if (userDetails instanceof EmployeePrincipal employeePrincipal) {
            int empNo = employeePrincipal.getEmpNo();
            employeeService.removeRefreshToken(empNo);
        } else if (userDetails instanceof AdminPrincipal adminPrincipal) {
            int adminNo = adminPrincipal.getAdmin().getAdminNo();
            adminService.removeRefreshToken(adminNo);
        } else {
            throw new IllegalArgumentException("올바르지 못한 유저입니다.");
        }
    }

    private void validateLoginRequest(String username, String password) {
        Map<String, String> errors = new HashMap<>();
        if ( username == null || username.isEmpty() ) {
            errors.put("username", "아이디를 입력해주세요.");
        }
        if ( password == null || password.isEmpty() ) {
            errors.put("password", "비밀번호를 입력해주세요.");
        }
        if (!errors.isEmpty()) {
            throw new InvalidLoginRequestException(errors);
        }
    }


}
