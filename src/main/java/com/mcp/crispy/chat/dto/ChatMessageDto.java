package com.mcp.crispy.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatMessageDto {
    private Integer msgNo;
    private String msgContent;
    private Date msgDt;
    private MsgStat msgStat;
    private Date createDt;
    private Integer creator;
    private Date modifyDt;
    private Integer modifier;
    private Integer chatRoomNo;
    private Integer empNo;
    private String empProfile; // 직원 프로필 이미지
    private String empName; // 직원 이름
}