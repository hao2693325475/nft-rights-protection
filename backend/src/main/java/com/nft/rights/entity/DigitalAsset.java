package com.nft.rights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("digital_assets")
public class DigitalAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tokenId;
    private String title;
    private String description;
    private String category;
    private String fileHash;
    private String phash;
    private String ipfsCid;
    private String metadataCid;
    private String creatorWallet;
    private String ownerWallet;
    private Integer royaltyBps;
    private String licenseType;
    private BigDecimal priceEth;
    private String status; // PENDING, AUDITED, MINTED, ON_SALE, TAKEN_DOWN
    private String txHash;
    private LocalDateTime createdAt;
}
