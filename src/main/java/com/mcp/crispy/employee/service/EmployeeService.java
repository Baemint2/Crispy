package com.mcp.crispy.employee.service;

import com.mcp.crispy.common.ImageService;
import com.mcp.crispy.common.validator.NewPasswordValidator;
import com.mcp.crispy.common.validator.PasswordChangeValidator;
import com.mcp.crispy.email.service.EmailService;
import com.mcp.crispy.employee.dto.*;
import com.mcp.crispy.employee.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.mcp.crispy.common.utils.RandomCodeUtils.generateTempPassword;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final EmailService emailService;
    private final OwnerNameService ownerNameService;
    private final ImageService imageService;
    @Lazy
    private final PasswordEncoder passwordEncoder;
    private final PasswordChangeValidator passwordChangeValidator;
    private final NewPasswordValidator newPasswordValidator;


    // 직원 아이디로 직원 정보 가져오기
     public EmployeeDto getEmployeeName(String username) {
        return employeeMapper.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("직원이 존재하지 않습니다."));
    }

    // 직원 전체 목록 가져오기 ( 현재 로그인한 자신 빼고 )
    public List<EmployeeDto> getAllEmployees(Integer empNo) {
        return employeeMapper.findAllExceptCurrentUser(empNo);
    }

    // 직원 번호로 직원 정보 가져오기
    public EmployeeDto getEmployeeDetailsByEmpNo(Integer empNo) {
        return employeeMapper.findByEmployeeDetailsByEmpNo(empNo)
                .orElseThrow(() -> new UsernameNotFoundException("직원이 존재하지 않습니다."));
    }

    // 직원 이메일과 아이디로 직원 정보 가져오기
    public FindEmployeeDto getEmpEmail(String empEmail, String empName) {
        return employeeMapper.findByEmpEmail(empEmail, empName)
                .orElseThrow(() -> new UsernameNotFoundException("이메일이 존재하지 않습니다."));
    }

    // 비밀번호 변경 ( 자신이 변경하는 거 )
    @Transactional
    public void updateEmployeePassword(String empId, PasswordChangeDto passwordChangeDto,Integer modifier) {
        EmployeeDto employeeDto = employeeMapper.findByUsername(empId)
                .orElseThrow(() -> new UsernameNotFoundException("직원이 존재하지 않습니다."));

        log.info("passwordChangeDto: {} {}", passwordChangeDto.getNewPassword(), passwordChangeDto.getConfirmPassword());
        passwordChangeValidator.validatePassword(passwordChangeDto, employeeDto);

        String encodedPassword = passwordEncoder.encode(passwordChangeDto.getNewPassword());
        employeeMapper.updateEmpPw(empId, encodedPassword, modifier);
    }

    // 비밀번호 변경 ( 자신이 변경하는 거 )
    @Transactional
    public void updateNewEmpPassword(String empId, NewPasswordDto newPasswordDto, Integer modifier) {
        EmployeeDto employeeDto = employeeMapper.findByUsername(empId)
                .orElseThrow(() -> new UsernameNotFoundException("직원이 존재하지 않습니다."));

        newPasswordValidator.validatePassword(newPasswordDto, employeeDto);

        String encodedPassword = passwordEncoder.encode(newPasswordDto.getNewPassword());
        employeeMapper.updateEmpPw(empId, encodedPassword, modifier);
    }



    // 임시 비밀번호로 변경 ( 관리자가 변경해주는 거)
    @Transactional
    public String resetEmployeePassword(String email, String empName, Integer modifier) {
        FindEmployeeDto employee = employeeMapper.findByEmpEmail(email, empName)
                .orElseThrow(() -> new UsernameNotFoundException("이메일이 존재하지 않습니다."));
        log.info("employee: {}", employee.getEmpEmail());
        String tempPassword = generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        employee.setEmpPw(encodedPassword);
        employeeMapper.updateEmpPw(employee.getEmpId(), encodedPassword, modifier);
        emailService.sendTempPasswordEmail(email, tempPassword);
        return tempPassword;
    }

    //  파라미터에 empId가 존재하면 비밀번호 찾기, empId가 존재하지 않으면 아이디 찾기
    public boolean checkEmployeeExists(String empName, String empEmail, String empId) {
        if (empId != null) {
            return employeeMapper.findByEmpNameAndEmpEmailAndEmpId(empName, empEmail, empId).isPresent();
        } else {
            return employeeMapper.findByEmpNameAndEmpEmail(empName, empEmail).isPresent();
        }
    }

    // 주소 정보 수정
    @Transactional
    public void updateAddress(EmpAddressUpdateDto employeeUpdateDto, Integer modifier) {
        int empNo = employeeUpdateDto.getEmpNo();

        employeeExists(empNo);
        employeeMapper.updateAddress(employeeUpdateDto, modifier);
    }


    // 서명 업데이트
    @Transactional
    public void updateEmpSign(EmployeeUpdateDto employeeUpdateDto) {
        String signData = employeeUpdateDto.getEmpSign();
        int empNo = employeeUpdateDto.getEmpNo();
        if (signData != null && !signData.isEmpty()) {
            try {
                String fileName = imageService.storeSignatureImage(signData, empNo);
                String storedUrl = "/emp_sign/" + fileName;
                employeeUpdateDto.setEmpSign(storedUrl);
            } catch (IOException e) {
                throw new RuntimeException("서명 저장에 실패했습니다.", e);
            }
        }

        employeeExists(empNo);
        employeeMapper.updateEmpSign(employeeUpdateDto);
    }

    public void updateEmpProfile(Integer empNo,
                                 MultipartFile file, Integer modifier) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("프로필 이미지 파일이 없습니다.");
        } else {
            String storedProfileImage = imageService.storeProfileImage(file);
            String storedUrl = "/profiles/" + storedProfileImage;

            employeeExists(empNo);
            employeeMapper.updateEmpProfile(storedUrl, empNo, modifier);
        }
    }

    // 전화번호 업데이트
    @Transactional
    public void changeEmpPhone(String empPhone, Integer empNo, Integer modifier) {
        employeeExists(empNo);
        employeeMapper.updateEmpPhone(empPhone, empNo, modifier);
    }

    // 이름 업데이트
    @Transactional
    public void changeEmpName(String empName, Integer empNo, Integer modifier) {
        log.info("changeEmpName: {} {}", empName, empNo);
        employeeExists(empNo);
        employeeMapper.updateEmpName(empName, empNo, modifier);
        ownerNameService.updateFrnOwnerIfEmployee(empNo, empName, modifier);
    }

    // 직책 변경
    @Transactional
    public void changePosNo(Integer posNo, Integer empNo, Integer modifier) {
        employeeExists(empNo);
        employeeMapper.updatePosNo(Position.of(posNo), empNo, modifier);
    }

    // 이메일 변경
    @Transactional
    public void changeEmail(String email, Integer empNo, Integer modifier) {
        employeeExists(empNo);
        employeeMapper.updateEmpEmail(email, empNo, modifier);
    }

    // 재직 상태 변경
    @Transactional
    public void changeEmpStat(Integer empStat, Integer empNo, Integer modifier) {
        employeeExists(empNo);
        employeeMapper.updateEmpStat(EmpStatus.of(empStat), empNo, modifier);
    }

    // 사용자 초대 검색 메소드
    public List<EmployeeDto> getSearchEmployees(EmployeeDto employeeDto, Integer empNo) {
        return employeeMapper.searchEmployees(employeeDto.getEmpName(), empNo);
    }

    // 사용자 초대 메소드
    public List<EmployeeDto> getInviteEmployees(Integer chatRoomNo) {
        return employeeMapper.inviteEmployees(chatRoomNo);
    }

    // 폼 수정 메소드
    @Transactional
    public void updateFormEmployee(EmployeeUpdateDto employeeUpdateDto, Integer modifier) {
        employeeMapper.updateFormEmployee(employeeUpdateDto, modifier);
    }

    // 아이디 중복 검증
    @Transactional
    public boolean existsByEmpId(String empId) {
        return employeeMapper.existsByEmpId(empId);
    }

    // 이메일 중복 검증
    @Transactional
    public boolean existsByEmail(String email) {
        return employeeMapper.existsByEmpEmail(email);
    }

    // 로그인시 리프레시 토큰 저장
    @Transactional
    public void updateRefreshToken(String refreshToken, int empNo) {
        employeeMapper.updateRefreshToken(refreshToken, empNo);
    }

    // 로그아웃시 리프레시 토큰 삭제
    @Transactional
    public void removeRefreshToken(int empNo) {
        employeeMapper.removeRefreshToken(empNo);
    }
    
    // 직원 연차 차감
    @Transactional
    public void updateAnnual(EmployeeDto empDto) {
    	employeeMapper.updateAnnual(empDto);
    }

    // 직원이 존재하는지 여부
    private void employeeExists(Integer empNo) {
        int count = employeeMapper.countByEmpNo(empNo);
        if (count <= 0) {
            throw new IllegalArgumentException("해당하는 직원이 존재하지 않습니다.");
        }
    }

}
