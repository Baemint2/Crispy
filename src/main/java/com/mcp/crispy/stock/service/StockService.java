package com.mcp.crispy.stock.service;


import com.mcp.crispy.approval.dto.ApprLineDto;
import com.mcp.crispy.approval.dto.ApprOptionDto;
import com.mcp.crispy.approval.dto.ApprStat;
import com.mcp.crispy.approval.dto.ApprovalDto;
import com.mcp.crispy.common.page.PageResponse;
import com.mcp.crispy.franchise.dto.FranchiseDto;
import com.mcp.crispy.franchise.service.FranchiseService;
import com.mcp.crispy.notification.dto.NotifyCt;
import com.mcp.crispy.notification.dto.NotifyDto;
import com.mcp.crispy.notification.service.NotificationService;
import com.mcp.crispy.stock.dto.StockDto;
import com.mcp.crispy.stock.dto.StockOptionDto;
import com.mcp.crispy.stock.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mcp.crispy.notification.service.SseService.ADMIN_NO;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;
    private final FranchiseService franchiseService;
    private final NotificationService notificationService;

    // 재고 현황 조회
    public PageResponse<StockDto> getStockList(StockOptionDto stockOptionDto, int limit) {

        if (limit == 0) {
            List<StockDto> items = stockMapper.getStockList(stockOptionDto);
            return new PageResponse<>(items, 0, 0, 0, 0);
        }

        // 현재 페이지 번호
        int page = Math.max(stockOptionDto.getPageNo(), 1);

        // 전체 재고 항목 수
        int totalCount = stockMapper.getStockCount(stockOptionDto);

        // 전체 페이지 수
        int totalPage = totalCount / limit + ((totalCount % limit > 0) ? 1 : 0);

        // 페이지 내비게이션 범위
        int startPage = ((page - 1) / limit) * limit + 1;
        int endPage = Math.min(startPage + limit - 1, totalPage);

        /*
        * 조회 범위
        *  - offset: 조회를 시작할 행의 인덱스
        *  - limit: 조회할 행의 개수
        */
        RowBounds rowBounds = new RowBounds(limit * (page - 1), limit);

        // 재고 항목 리스트
        List<StockDto> items = stockMapper.getStockList(stockOptionDto, rowBounds);

        // PageResponse 객체
        return new PageResponse<>(items, totalPage, startPage, endPage, page);

    }

    // 재고 카테고리 목록 조회
    public List<StockDto> getStockCtList() {
        return stockMapper.getStockCtList();
    }

    // 재고 상세 조회
    public StockDto getStockDetail(int stockNo) {
        return stockMapper.getStockDetail(stockNo);
    }

    // 발주 재고 목록 조회
    public List<StockDto> getStockList(List<Integer> stockNoList) {
        return stockMapper.getSelectStock(stockNoList);
    }

    // 임시저장 값 존재 여부 확인
    public int checkOrderTemp(int empNo) {
        return stockMapper.checkOrderTemp(empNo);
    }

    // 발주 재고 임시저장
    @Transactional
    public int insertOrderTemp(ApprovalDto approvalDto) {
        stockMapper.deleteOrderTemp(approvalDto.getEmpNo()); // 이전 임시저장 내용 삭제
        return stockMapper.insertOrderTemp(approvalDto);
    }
    
    // 임시저장 내용 불러오기
    public List<StockDto> getOrderTemp(int empNo) {
        return stockMapper.getOrderTemp(empNo);
    }

    // 발주 신청
    @Transactional
    public int insertOrderAppr(ApprovalDto approvalDto) {

        stockMapper.insertApproval(approvalDto);
        int apprNo = approvalDto.getApprNo();

        log.info("insertOrderAppr: {}", approvalDto.getEmpNo());
        stockMapper.insertOrder(approvalDto);
        // ApprLineDto 업데이트
        ApprLineDto apprLineDto = ApprLineDto.builder()
                .apprLineOrder(0)
                .apprNo(apprNo)
                .empNo(approvalDto.getEmpNo())
                .creator(approvalDto.getEmpNo())
                .build();
        log.info("apprLineDto: {}", approvalDto.getEmpNo());

        stockMapper.insertStockOrder(approvalDto);

        stockMapper.insertApprLine(apprLineDto);

        // 결재자에게 알림 전송
        notifyApprovalRequest(approvalDto);

        return 1;

    }

    private void notifyApprovalRequest(ApprovalDto approvalDto) {
        FranchiseDto frn = franchiseService.getFrnByEmpNo(approvalDto.getEmpNo());
        NotifyCt notifyCt = NotifyCt.ORDER;
        NotifyDto notifyDto = NotifyDto.builder()
                .notifyCt(notifyCt)
                .notifyContent(frn.getFrnName() + "에서 " + notifyCt.getDescription() +"결재를 요청하였습니다.")
                .apprType("approval") // 추가
                .timeOffType("sign") // 추가
                .documentType("stock-order")
                .build();

        // 알림 전송
        notificationService.sendApprovalNotification(notifyDto, ADMIN_NO);
    }

    // 발주 신청 목록 조회 (가맹점주, 관리자)
    public PageResponse<ApprovalDto> getOrderApprList(ApprOptionDto apprOptionDto, int limit) {

        int page = Math.max(apprOptionDto.getPageNo(), 1);
        int totalCount = stockMapper.getOrderApprCount(apprOptionDto);
        int totalPage = totalCount / limit + ((totalCount % limit > 0) ? 1 : 0);
        int startPage = ((page - 1) / limit) * limit + 1;
        int endPage = Math.min(startPage + limit - 1, totalPage);

        RowBounds rowBounds = new RowBounds(limit * (page - 1), limit);
        List<ApprovalDto> items = stockMapper.getOrderApprList(apprOptionDto, rowBounds);

        for (ApprovalDto approvalDto : items) {

            // 문서상태명 설정 (대기, 진행중, 승인, 반려)
            int apprStat = approvalDto.getApprStat();
            String apprStatName = ApprStat.of(apprStat).getDesciption();
            approvalDto.setApprStatName(apprStatName);

        }

        return new PageResponse<>(items, totalPage, startPage, endPage, page);

    }

}
