package com.nft.rights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("audit_records")
public class AuditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private String fileHash;
    private String phash;
    private Boolean exactMatch;
    private BigDecimal maxSimilarity;
    private Long matchedAssetId;
    private Boolean passFlag;
    private String auditRemark;
    private LocalDateTime auditedAt;
}
