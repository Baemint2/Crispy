package com.mcp.crispy.freeboard.service;

import com.mcp.crispy.common.page.PageResponse;
import com.mcp.crispy.common.utils.MyFileUtils;
import com.mcp.crispy.freeboard.dto.FreeBoardDto;
import com.mcp.crispy.freeboard.dto.FreeBoardFileDto;
import com.mcp.crispy.freeboard.mapper.FreeBoardMapper;
import com.vane.badwordfiltering.BadWordFiltering;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.mcp.crispy.comment.service.BadWordFilteringHelper.getBadWordFiltering;


@Slf4j
@Service
@RequiredArgsConstructor
public class FreeBoardService {

    private final FreeBoardMapper freeBoardMapper;
    private final MyFileUtils myFileUtils;

    // 게시판 생성
    @Transactional
    public int insertFreeBoard(FreeBoardDto freeBoardDto, Integer empNo, List<MultipartFile> files) {

        FreeBoardDto board = FreeBoardDto.builder()
                .empNo(empNo)
                .boardTitle(freeBoardDto.getBoardTitle())
                .boardContent(freeBoardDto.getBoardContent())
                .build();
        freeBoardMapper.insertFreeBoard(board);


        if (files != null && !files.isEmpty()) {
            registerFreeBoardFile(files, board.getBoardNo());
        }
        return board.getBoardNo();
    }

    // 게시판 수정
    @Transactional
    public int updateFreeBoard(FreeBoardDto freeBoardDto, Integer empNo, List<Integer> deletedFileNo, List<MultipartFile> newFiles) {

        FreeBoardDto board = FreeBoardDto.builder()
                .boardNo(freeBoardDto.getBoardNo())
                .boardTitle(freeBoardDto.getBoardTitle())
                .boardContent(freeBoardDto.getBoardContent())
                .boardCtNo(freeBoardDto.getBoardCtNo())
                .modifier(empNo)
                .build();
        freeBoardMapper.updateFreeBoard(board);

        if (deletedFileNo != null && !deletedFileNo.isEmpty()) {
            for (Integer fileNo : deletedFileNo) {
                FreeBoardFileDto file = freeBoardMapper.getFreeBoardFileByNo(fileNo);
                if (file != null) {
                    File fileToDelete = new File(file.getBoardPath(), file.getBoardRename());
                    if (fileToDelete.exists()) {
                        fileToDelete.delete();
                    }
                    freeBoardMapper.deleteFreeBoardFile(fileNo);
                }
            }
        }
        // 새로운 파일 추가
        if (newFiles != null && !newFiles.isEmpty()) {
            registerFreeBoardFile(newFiles, board.getBoardNo());
        }
        return board.getBoardNo();
    }

    // 게시판 삭제
    public void deleteFreeBoard(Integer boardNo, Integer modifier) {
        log.info("Delete Board : {} {}", boardNo, modifier);
        freeBoardMapper.deleteFreeBoard(boardNo, modifier);
    }

    // 자유게시판 LIST
    public PageResponse<FreeBoardDto> getFree2BoardList(FreeBoardDto freeBoardDto, int limit) {
        BadWordFiltering badWordFiltering = getBadWordFiltering();
        // 현재 페이지 번호
        int page = Math.max(freeBoardDto.getPageNo(), 1);

        // 전체 재고 항목 수
        int totalCount = freeBoardMapper.getFreeBoardCount();

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

        // 게시판 리스트
        List<FreeBoardDto> items = freeBoardMapper.getFreeBoardList(freeBoardDto, rowBounds);
        items.forEach(item -> {
            String boardTitle = badWordFiltering.change(item.getBoardTitle());

            if (item.getBoardContent() != null) {
                String boardContent = badWordFiltering.change(item.getBoardContent());
                item.setBoardContent(boardContent);
            }

            item.setBoardTitle(boardTitle);
        });

        // PageResponse 객체
        return new PageResponse<>(items, totalPage, startPage, endPage, page);
    }

    @Transactional(readOnly=true)
    public FreeBoardDto loadFreeBoardByNo(int boardNo, int empNo) {
        BadWordFiltering badWordFiltering = getBadWordFiltering();
        log.info("Load board by no: {}", boardNo);
        FreeBoardDto freeBoardDto = freeBoardMapper.getFreeBoardByNo(boardNo);
        log.info("Load Board : {}", freeBoardDto);
        if (freeBoardDto.isHasAttachment()) {
            log.info("Load Board attachment");
            List<FreeBoardFileDto> files = freeBoardMapper.getFreeBoardFileList(boardNo);
            freeBoardDto.setFiles(files);
        }

        // 게시판 제목과 내용을 필터링
        freeBoardDto.setBoardTitle(badWordFiltering.change(freeBoardDto.getBoardTitle()));

        if (freeBoardDto.getBoardContent() != null) {
            freeBoardDto.setBoardContent(badWordFiltering.change(freeBoardDto.getBoardContent()));
        }

        boolean isLiked = freeBoardMapper.isLiked(boardNo, empNo) > 0;
        freeBoardDto.setLiked(isLiked);

        return freeBoardDto;
    }



    // BoardService 클래스에 registerBoardFile 메서드 수정
    @Transactional
    public boolean registerFreeBoardFile(List<MultipartFile> files, int boardNo) {
        int insertBoardFileCount = 0;
        for (MultipartFile multipartFile: files) {
            if (multipartFile != null && !multipartFile.isEmpty()) {
                try {
                    String boardPath = myFileUtils.getBoardPath();
                    File dir = new File(boardPath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    String boardOrigin = multipartFile.getOriginalFilename();
                    String boardRename = myFileUtils.getBoardRename(boardOrigin);
                    File file = new File(boardPath, boardRename);
                    multipartFile.transferTo(file);

                    FreeBoardFileDto boardFile = FreeBoardFileDto.builder()
                            .boardRename(boardRename)
                            .boardOrigin(boardOrigin)
                            .boardPath(boardPath)
                            .boardNo(boardNo)
                            .build();

                    int inserted = freeBoardMapper.insertFreeBoardFile(boardFile);
                    if (inserted == 1) {
                        insertBoardFileCount++;
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return insertBoardFileCount == files.size();
    }

    // 좋아요 토글
    @Transactional
    public void toggleFreeBoardLike(int boardNo, int empNo) {
        FreeBoardDto freeBoardDto = freeBoardMapper.getFreeBoardByNo(boardNo);
        if (freeBoardDto == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        int likeStatus = freeBoardMapper.isLiked(boardNo, empNo);
        if (likeStatus == 0) {
            freeBoardDto.addLike();
            freeBoardMapper.addLike(boardNo, empNo);
        } else {
            freeBoardDto.removeLike();
            freeBoardMapper.removeLike(boardNo, empNo);
        }

        freeBoardMapper.updateLikeCount(freeBoardDto.getBoardLikeCount(), boardNo);
    }

    // 조회수 증가
    @Transactional
    public void increaseFreeBoardHit(int boardNo, HttpServletRequest request, HttpServletResponse response) {
        FreeBoardDto board = freeBoardMapper.getFreeBoardByNo(boardNo);


        Cookie[] cookies = request.getCookies();
        Cookie hitCookie = null;
        boolean isCookieUpdated = false;

        // 쿠키가 존재하는 경우
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("boardHit")) {
                    hitCookie = cookie;
                    // 쿠키의 값에 현재 게시물 번호가 포함이 안 되어 있으면 진입, 있으면 바로 break
                    if (!cookie.getValue().contains("[" + boardNo + "]")) {
                        freeBoardMapper.increaseBoardHit(boardNo); // DB 업데이트
                        board.addBoardHit();
                        cookie.setValue(cookie.getValue() + "[" + boardNo + "]");
                        isCookieUpdated = true;
                    }
                    break;
                }
            }
        }

        // boardHit 쿠키가 존재하지 않는 경우
        if (hitCookie == null) {
            freeBoardMapper.increaseBoardHit(boardNo); // DB 업데이트
            board.addBoardHit();
            hitCookie = new Cookie("boardHit", "[" + boardNo + "]");
            isCookieUpdated = true;
        }

        // 쿠키가 갱신된 경우
        if (isCookieUpdated) {
            int maxAge = getEndOfDay(); // 자정을 기준으로 조회수 쿠키 초기화
            hitCookie.setPath("/");
            hitCookie.setMaxAge(maxAge);
            response.addCookie(hitCookie);
        }

    }

    // 현재 시간부터 자정까지 남은 초 계산
    private int getEndOfDay() {
        long todayEndSecond = LocalDate.now().atTime(LocalTime.MAX).toEpochSecond(ZoneOffset.UTC);
        long currentSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

        return (int) (todayEndSecond - currentSecond);
    }

}
