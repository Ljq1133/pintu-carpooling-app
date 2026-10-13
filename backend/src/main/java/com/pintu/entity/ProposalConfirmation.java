package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("proposal_confirmation")
public class ProposalConfirmation {
    @TableId(type = IdType.INPUT)
    private String id;
    private String proposalId;
    private String userId;
    private String role;
    private LocalDateTime confirmedAt;
}
