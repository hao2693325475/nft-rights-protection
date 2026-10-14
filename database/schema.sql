-- ========================================================
-- 基于区块链的NFT数字资产权益保护系统 - 数据库初始化脚本
-- MySQL 8.0+
-- ========================================================

CREATE DATABASE IF NOT EXISTS `nft_rights_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `nft_rights_db`;

-- 1. 用户表 (users)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `wallet_address` VARCHAR(42) NOT NULL UNIQUE COMMENT '以太坊钱包地址(0x...)',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名/昵称',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名(脱敏)',
  `id_card_hash` VARCHAR(64) DEFAULT NULL COMMENT '身份证件哈希(SHA-256)',
  `role` VARCHAR(20) NOT NULL DEFAULT 'CREATOR' COMMENT '角色: CREATOR(创作者), PURCHASER(购买者), ADMIN(管理员)',
  `avatar_cid` VARCHAR(100) DEFAULT NULL COMMENT 'IPFS头像CID',
  `is_verified` TINYINT(1) DEFAULT 0 COMMENT '实名认证状态: 0未认证, 1已认证',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户与钱包映射表';

-- 2. 数字资产表 (digital_assets)
DROP TABLE IF EXISTS `digital_assets`;
CREATE TABLE `digital_assets` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统资产ID',
  `token_id` BIGINT DEFAULT NULL UNIQUE COMMENT '链上Token ID (ERC-721)',
  `title` VARCHAR(100) NOT NULL COMMENT '作品名称',
  `description` TEXT DEFAULT NULL COMMENT '作品描述',
  `category` VARCHAR(50) DEFAULT 'ART' COMMENT '分类: ART(数字艺术), MUSIC(音乐作品), COLLECTIBLE(收藏品), OTHER',
  `file_hash` VARCHAR(128) NOT NULL UNIQUE COMMENT '作品源文件SHA-256哈希指纹',
  `phash` VARCHAR(64) DEFAULT NULL COMMENT '感知哈希指纹(用于相似度查重)',
  `ipfs_cid` VARCHAR(100) NOT NULL COMMENT '原文件IPFS存储CID',
  `metadata_cid` VARCHAR(100) NOT NULL COMMENT 'ERC-721 Metadata JSON IPFS CID',
  `creator_wallet` VARCHAR(42) NOT NULL COMMENT '原创作者钱包地址',
  `owner_wallet` VARCHAR(42) NOT NULL COMMENT '当前持有者钱包地址',
  `royalty_bps` INT NOT NULL DEFAULT 500 COMMENT 'EIP-2981版税基点(500 = 5%)',
  `license_type` VARCHAR(50) DEFAULT 'COMMERCIAL' COMMENT '授权类型: VIEW_ONLY, COMMERCIAL, EXCLUSIVE',
  `price_eth` DECIMAL(18, 8) DEFAULT 0.00000000 COMMENT '挂牌标价(ETH)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING(待预审), AUDITED(预审通过), MINTED(已铸造), ON_SALE(挂牌售卖), TAKEN_DOWN(违规下架)',
  `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '链上铸造交易哈希',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
  INDEX `idx_creator_wallet` (`creator_wallet`),
  INDEX `idx_owner_wallet` (`owner_wallet`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字资产主表';

-- 3. 交易记录表 (transaction_records)
DROP TABLE IF EXISTS `transaction_records`;
CREATE TABLE `transaction_records` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `tx_hash` VARCHAR(66) NOT NULL UNIQUE COMMENT '链上交易哈希',
  `token_id` BIGINT NOT NULL COMMENT '代币ID',
  `seller_wallet` VARCHAR(42) NOT NULL COMMENT '卖家钱包地址',
  `buyer_wallet` VARCHAR(42) NOT NULL COMMENT '买家钱包地址',
  `price_eth` DECIMAL(18, 8) NOT NULL COMMENT '成交总金额(ETH)',
  `royalty_eth` DECIMAL(18, 8) NOT NULL COMMENT 'EIP-2981自动分配版税(ETH)',
  `seller_revenue_eth` DECIMAL(18, 8) NOT NULL COMMENT '卖家净收益(ETH)',
  `block_number` BIGINT NOT NULL COMMENT '区块高度',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交易完成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链上版权交易流转日志';

-- 4. 版税分成明细表 (royalty_distributions)
DROP TABLE IF EXISTS `royalty_distributions`;
CREATE TABLE `royalty_distributions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `token_id` BIGINT NOT NULL COMMENT '资产Token ID',
  `creator_wallet` VARCHAR(42) NOT NULL COMMENT '收益接收创作者钱包',
  `amount_eth` DECIMAL(18, 8) NOT NULL COMMENT '结算版税金额(ETH)',
  `tx_hash` VARCHAR(66) NOT NULL COMMENT '触发版税的交易哈希',
  `status` VARCHAR(20) DEFAULT 'CLAIMED' COMMENT '状态: PENDING_CLAIM(待提取), CLAIMED(已结算到账)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分成结算时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版税收益自动分成记录';

-- 5. 链下查重复审记录表 (audit_records)
DROP TABLE IF EXISTS `audit_records`;
CREATE TABLE `audit_records` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `asset_id` BIGINT NOT NULL COMMENT '关联预审资产ID',
  `file_hash` VARCHAR(64) NOT NULL COMMENT '待审文件SHA-256',
  `phash` VARCHAR(64) DEFAULT NULL COMMENT '待审文件pHash',
  `exact_match` TINYINT(1) DEFAULT 0 COMMENT '是否存在SHA-256完全重复: 0无, 1有',
  `max_similarity` DECIMAL(5, 2) DEFAULT 0.00 COMMENT '最高图文相似度百分比(0-100%)',
  `matched_asset_id` BIGINT DEFAULT NULL COMMENT '相似冲突资产ID',
  `pass_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '审核结果: 1通过, 0驳回',
  `audit_remark` VARCHAR(255) DEFAULT '自动预审通过: 未检测到碰撞与高相似度' COMMENT '审核意见',
  `audited_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链下查重与相似度预审日志';

-- 6. 侵权举报与证据导出表 (infringement_reports)
DROP TABLE IF EXISTS `infringement_reports`;
CREATE TABLE `infringement_reports` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `token_id` BIGINT NOT NULL COMMENT '被举报资产Token ID',
  `reporter_wallet` VARCHAR(42) NOT NULL COMMENT '举报人钱包',
  `reason` TEXT NOT NULL COMMENT '侵权事实描述与理由',
  `evidence_ipfs_cid` VARCHAR(100) DEFAULT NULL COMMENT '举证材料IPFS包CID',
  `evidence_package_json` JSON DEFAULT NULL COMMENT '生成的司法电子证据链JSON结构',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING(待处理), CONFIRMED(侵权属实), REJECTED(驳回举报)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='侵权举报与证据链记录';

-- 插入初始化测试数据
INSERT INTO `users` (`wallet_address`, `username`, `real_name`, `role`, `is_verified`) VALUES
('0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', '创作者_谷俊豪', '谷俊豪', 'CREATOR', 1),
('0x70997970C51812dc3A010C7d01b50e0d17dc79C8', '藏家_Alice', 'Alice', 'PURCHASER', 1),
('0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC', '系统管理员', '管理员', 'ADMIN', 1);

INSERT INTO `digital_assets` (`token_id`, `title`, `description`, `category`, `file_hash`, `phash`, `ipfs_cid`, `metadata_cid`, `creator_wallet`, `owner_wallet`, `royalty_bps`, `price_eth`, `status`, `tx_hash`) VALUES
(1, '星空璀璨·区块链创想', '基于AIGC生成并存证的赛博朋克艺术作品，具有永久可追溯版权凭证。', 'ART', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'd41d8cd98f00b204e9800998ecf8427e', 'QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco', 'QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 1000, 0.50000000, 'ON_SALE', '0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef');
