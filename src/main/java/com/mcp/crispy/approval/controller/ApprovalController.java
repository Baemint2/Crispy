package com.mcp.crispy.approval.controller;

import com.mcp.crispy.approval.dto.ApplicantDto;
import com.mcp.crispy.approval.dto.ApprLineDto;
import com.mcp.crispy.approval.dto.ApprOptionDto;
import com.mcp.crispy.approval.dto.ApprovalDto;
import com.mcp.crispy.approval.service.ApprovalService;
import com.mcp.crispy.auth.domain.EmployeePrincipal;
import com.mcp.crispy.common.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("crispy")
@RequiredArgsConstructor
public class ApprovalController {

	private final ApprovalService approvalService;

	/**
	 * 휴가, 휴직 신청 페이지
	 * 우혜진 (24. 05. 20.)
	 *
	 * @param authentication
	 * @param model
	 * @return forward (time-off-approval.html)
	 */
	@GetMapping("time-off-approval")
	public String timeOffAppr(Authentication authentication,
							  Model model) {

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		int frnNo = userDetails.getFrnNo();
		int empNo = userDetails.getEmpNo();

		List<ApprLineDto> apprLineDtoList = approvalService.getApprLine(frnNo, empNo);
		model.addAttribute("apprLineDtoList", apprLineDtoList);

		return "approval/time-off-approval";

	}

	/**
	 * 문서 양식 변경
	 * 우혜진 (24. 06. 06.)
	 *
	 * @param timeOffCtNo
	 * @return result
	 */
	@GetMapping("change-time-off-ct")
	public String changeTimeOffCt(@RequestParam("timeOffCtNo") int timeOffCtNo) {

		String path;

		switch (timeOffCtNo) {
			case 0: path = "document/vacation-req :: vacation-req"; break;
			case 1: path = "document/leave-of-absence-req :: leave-of-absence-req"; break;
			default: path = "";
		}

		return path;

	}

	/**
	 * 직원 정보 조회
	 * 우혜진 (24. 06. 05.)
	 *
	 * @param authentication
	 * @return result
	 */
	@GetMapping("get-emp-info")
	public ResponseEntity<ApplicantDto> getEmpInfo(Authentication authentication) {
		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		return ResponseEntity.ok(approvalService.getEmpInfo(userDetails.getEmpNo()));
	}

	/**
	 * 임시저장 값 존재 여부 확인
	 * 우혜진 (24. 06. 05.)
	 *
	 * @param authentication
	 * @param timeOffCtNo
	 * @return result
	 */
	@GetMapping("check-time-off-temp")
	public ResponseEntity<?> checkTimeOffTemp(Authentication authentication,
											  @RequestParam("timeOffCtNo") int timeOffCtNo) {
		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		return ResponseEntity.ok(approvalService.checkTimeOffTemp(userDetails.getEmpNo(), timeOffCtNo));
	}

	/**
	 * 휴가, 휴직 임시저장
	 * 우혜진 (24. 06. 06.)
	 *
	 * @param authentication
	 * @param approvalDto
	 * @return result
	 */
	@PostMapping("time-off-temp")
	public ResponseEntity<?> timeOffTemp(Authentication authentication,
										 @RequestBody @ModelAttribute ApprovalDto approvalDto) {

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		approvalDto.setEmpNo(userDetails.getEmpNo());

		return ResponseEntity.ok(approvalService.insertTimeOffTemp(approvalDto));

	}

	/**
	 * 임시저장 내용 불러오기
	 * 우혜진 (24. 06. 07)
	 *
	 * @param authentication
	 * @param timeOffCtNo
	 * @param model
	 * @return result
	 */
	@GetMapping("get-time-off-temp")
	public String getTimeOffTemp(Authentication authentication,
								 @RequestParam("timeOffCtNo") int timeOffCtNo,
								 Model model) {

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();

		ApprovalDto approvalDto = approvalService.getTimeOffTemp(userDetails.getEmpNo(), timeOffCtNo);
		model.addAttribute("approvalDto", approvalDto);

		String path;

		switch (timeOffCtNo) {
			case 0: path = "document/vacation-req :: vacation-req"; break;
			case 1: path = "document/leave-of-absence-req :: leave-of-absence-req"; break;
			default: path = "";
		}

		return path;

	}

	/**
	 * 휴가, 휴직 신청
	 * 우혜진 (24. 06. 09.)
	 *
	 * @param authentication
	 * @param approvalDto
	 * @param ra
	 * @return redirect (timeOffApprList()) or redirect (timeOffAppr())
	 */
	@PostMapping("insert-time-off-appr")
	public String insertTimeOffAppr(Authentication authentication,
									@ModelAttribute ApprovalDto approvalDto,
									RedirectAttributes ra) throws IOException {

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		approvalDto.setEmpNo(userDetails.getEmpNo());

		int result = approvalService.insertTimeOffAppr(approvalDto);

		if (result == 1) {
			ra.addFlashAttribute("resultMsg", "결재 신청이 완료되었습니다.");
			return "redirect:/crispy/approval-list/draft";

		} else {
			ra.addFlashAttribute("resultMsg", "결재 신청이 완료되지 않았습니다.");
			return "redirect:/crispy/time-off-approval";
		}

	}

	/**
	 * 결재 문서 목록 조회 (휴가,휴직 신청서)
	 * 우혜진 (24. 06. 11.)
	 *
	 * @param authentication
	 * @param apprOptionDto
	 * @param type
	 * @param model
	 * @return forward (approval-list.html)
	 */
	@GetMapping("approval-list/{type:^(?:draft|sign)$}")
	public String timeOffApprList(Authentication authentication,
								  ApprOptionDto apprOptionDto,
								  @PathVariable("type") String type,
								  Model model) {

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		apprOptionDto.setEmpNo(userDetails.getEmpNo());

		apprOptionDto.setType(type);
		apprOptionDto.setTimeOffCtNo(-1);
		apprOptionDto.setApprStat(-1);

		PageResponse<ApprovalDto> approvalDtoList = approvalService.getTimeOffApprList(apprOptionDto, 10);
		model.addAttribute("approvalDtoList", approvalDtoList);

		return "approval/approval-list";

	}

	/**
	 * 결재 문서 항목 조회 (휴가,휴직 신청서)
	 * 우혜진 (24. 06. 11.)
	 *
	 * @param authentication
	 * @param apprOptionDto
	 * @param type
	 * @param model
	 * @return result
	 */
	@GetMapping("approval-items/{type:^(?:draft|sign)$}")
	public String timeOffApprItems(Authentication authentication,
								   ApprOptionDto apprOptionDto,
								   @PathVariable("type") String type,
								   Model model) {

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		apprOptionDto.setEmpNo(userDetails.getEmpNo());

		apprOptionDto.setType(type);
		PageResponse<ApprovalDto> approvalDtoList = approvalService.getTimeOffApprList(apprOptionDto, 10);
		model.addAttribute("approvalDtoList", approvalDtoList);

		return "approval/approval-list :: appr-list-container";

	}

	/**
	 * 결재 문서 상세 조회
	 * 우혜진 (24. 06. 11.)
	 *
	 * @param authentication
	 * @param apprType
	 * @param apprNo
	 * @param model
	 * @return forward (approval-detail.html)
	 */
	@GetMapping("approval-detail/{apprType:^(?:time-off|stock-order)$}/{apprNo}")
	public String timeOffApprDetail(@PathVariable("apprType") String apprType,
									@PathVariable("apprNo") int apprNo,
									Authentication authentication,
									Model model) {

		log.info("apprType: {}", apprType);
		ApprovalDto approvalDto;

		// 발주 신청서
		if (apprType.equals("stock-order")) {

			approvalDto = approvalService.getStockOrderApprDetail(apprNo);
			model.addAttribute("approvalDto", approvalDto);

			// 가맹점주 신청 내역에서 결재하기 버튼 숨기기
			if (authentication.getPrincipal() instanceof EmployeePrincipal)
				model.addAttribute("apprBtn", "none");

			// 관리자가 결재 완료 상태일 때 결재하기 버튼 숨기기
			if (approvalDto.getApprLineStat() > 0)
				model.addAttribute("apprBtn", "none");

			return "approval/approval-detail";

		}

		// 휴가,휴직 신청서
		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		approvalDto = approvalService.getTimeOffApprDetail(userDetails.getEmpNo(), apprNo);

		if (apprType.equals("time-off")) {

			// 기안함에서 결재하기 버튼 숨기기
			if (approvalDto.getEmpNo() == userDetails.getEmpNo())
				model.addAttribute("apprBtn", "none");

			// 결재함에서 결재 완료 상태일 때 결재하기 버튼 숨기기
			approvalDto.getApprLineDtoList().forEach(apprLineDto -> {
				if (apprLineDto.getEmpNo() == userDetails.getEmpNo() && apprLineDto.getApprLineStat() != 0)
					model.addAttribute("apprBtn", "none");
			});

		}

		model.addAttribute("approvalDto", approvalDto);
		return "approval/approval-detail";

	}

	/**
	 * 문서 결재
	 * 우혜진 (24. 06. 15.)
	 *
	 * @param map
	 * @param authentication
	 * @return result
	 * @throws IOException
	 */
	@PutMapping("change-appr-line-stat")
	public ResponseEntity<?> changeApprLineStat(@RequestBody Map<String, Object> map,
												Authentication authentication) throws IOException {

		if (map.get("apprType").toString().equals("stock-order"))
			return ResponseEntity.ok(approvalService.changeApprLineStat(map));

		EmployeePrincipal userDetails = (EmployeePrincipal) authentication.getPrincipal();
		map.put("empNo", userDetails.getEmpNo());
		return ResponseEntity.ok(approvalService.changeApprLineStat(map));

	}

}
