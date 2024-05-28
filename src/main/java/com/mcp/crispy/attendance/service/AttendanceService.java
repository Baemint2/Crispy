package com.mcp.crispy.attendance.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mcp.crispy.attendance.dto.AttendanceDto;
import com.mcp.crispy.attendance.mapper.AttendanceMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

	private final AttendanceMapper attendanceMapper;

	@Transactional
	public int insertAttendance(AttendanceDto attendanceDto)
	{
		return attendanceMapper.insertAttendance(attendanceDto);
	}
	
	@Transactional(readOnly = true)
	public List<AttendanceDto> getAttList() {
		List<AttendanceDto> attenList = attendanceMapper.getAttList(); 
		String workingTimeForm;
		for(int i = 0; i < attenList.size(); i++) {
			
			workingTimeForm = attenList.get(i).getAttWorkTime().substring(0 ,2) + "h" +  
								attenList.get(i).getAttWorkTime().substring(3 , 5)+ "m" +
									attenList.get(i).getAttWorkTime().substring(6 , 8) + "s";
			
			attenList.get(i).setAttWorkTime(workingTimeForm);
		}
		
		
		return attenList;
	}
}