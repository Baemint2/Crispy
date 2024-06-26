package com.mcp.crispy.attendance.controller;

import com.mcp.crispy.attendance.dto.AttendanceDto;
import com.mcp.crispy.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@RequestMapping("/crispy")
public class AttendanceController {
	private final AttendanceService attendanceService;

	@PostMapping(value="/registAtt", produces = "application/json")
	public ResponseEntity<Integer> insertAttendance(@RequestBody AttendanceDto attendanceDto, Principal principal)
	{	
		int insertCount = attendanceService.insertAttendance(attendanceDto);
		return ResponseEntity.ok(insertCount);
	}
	
	@ResponseBody
	@GetMapping(value="/getAttendList", produces="application/json")
	public List<AttendanceDto> getAttList(@RequestParam("month") int month, @RequestParam("empNo") int empNo) {
		Map<String, Object> params = new HashMap<>();
		params.put("month", month);
		params.put("empNo", empNo);
	    return attendanceService.getAttList(params);
	}
	
}
