package com.mcp.crispy.employee.service;

import com.mcp.crispy.employee.dto.EmpStatus;
import com.mcp.crispy.employee.dto.EmployeeDto;
import com.mcp.crispy.employee.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
class EmployeeServiceTest {
    @Autowired
    EmployeeMapper employeeMapper;
    @Autowired EmployeeService employeeService;

    @Test
    void 직원직책변경() {
        //직책 변경
        EmpStatus empStatus = EmpStatus.EMPLOYED;
        Integer empNo = 22;
        employeeService.changeEmpStat(empStatus.getValue(), empNo, empNo);
        EmployeeDto emp = employeeService.getEmployeeDetailsByEmpNo(empNo);
        assertThat(emp.getEmpStat()).isEqualTo(empStatus);
    }

    @Test
    void 직원번호변경() {
        Integer empNo = 22;
        employeeService.changeEmpPhone("01012341234", empNo, empNo);
        EmployeeDto emp = employeeService.getEmployeeDetailsByEmpNo(empNo);
        assertThat(emp.getEmpPhone()).isEqualTo("01012341234");
    }
}