package com.mcp.crispy.employee.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.mcp.crispy.employee.dto.EmployeeDto;


@Mapper
public interface EmployeeMapper {

	
	EmployeeDto findByMemberEmail(String userEmail);
}