package com.nft.rights.service;

import com.nft.rights.entity.AuditRecord;
import com.nft.rights.entity.DigitalAsset;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 链下预审与确权服务
 * 1. 计算 SHA-256 文件指纹，完全碰撞拦截
 * 2. 计算感知哈希 (pHash) 与汉明距离 (Hamming Distance)，检测相似二次加工侵权
 */
@Service
public class PreCheckService {

    /**
     * 计算字节数组的 SHA-256 哈希
     */
    public String calculateSHA256(byte[] fileBytes) {
        return DigestUtils.sha256Hex(fileBytes);
    }

    /**
     * 模拟计算图像感知哈希 (pHash - 64位十六进制字符串)
     */
    public String calculatePerceptualHash(byte[] fileBytes) {
        // 使用文件摘要的 MD5 前 16 位模拟 64位二进制 pHash
        return DigestUtils.md5Hex(fileBytes).substring(0, 16);
    }

    /**
     * 计算两个 64位 pHash 之间的汉明距离
     */
    public int calculateHammingDistance(String phash1, String phash2) {
        if (phash1 == null || phash2 == null || phash1.length() != phash2.length()) {
            return 64; // 无可比性，返回最大距离
        }
        int distance = 0;
        for (int i = 0; i < phash1.length(); i++) {
            char c1 = phash1.charAt(i);
            char c2 = phash2.charAt(i);
            if (c1 != c2) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * 链下查重预审核心逻辑
     */
    public AuditRecord performPreCheck(byte[] fileBytes, List<DigitalAsset> existingAssets) {
        String sha256 = calculateSHA256(fileBytes);
        String pHash = calculatePerceptualHash(fileBytes);

        AuditRecord record = new AuditRecord();
        record.setFileHash(sha256);
        record.setPhash(pHash);
        record.setExactMatch(false);
        record.setMaxSimilarity(BigDecimal.ZERO);
        record.setPassFlag(true);
        record.setAuditRemark("预审通过: 未检测到哈希碰撞与高相似度侵权风险");

        for (DigitalAsset asset : existingAssets) {
            // 1. 完全哈希碰撞检测
            if (sha256.equalsIgnoreCase(asset.getFileHash())) {
                record.setExactMatch(true);
                record.setMatchedAssetId(asset.getId());
                record.setPassFlag(false);
                record.setMaxSimilarity(new BigDecimal("100.00"));
                record.setAuditRemark("预审驳回: 检测到相同作品哈希碰撞，源文件已存在 (Asset ID: " + asset.getId() + ")");
                return record;
            }

            // 2. 感知哈希相似度检测 (汉明距离)
            if (asset.getPhash() != null) {
                int dist = calculateHammingDistance(pHash, asset.getPhash());
                // 相似度百分比估算: (16 - dist) / 16
                double similarity = ((16.0 - dist) / 16.0) * 100.0;
                BigDecimal simBD = BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP);

                if (simBD.compareTo(record.getMaxSimilarity()) > 0) {
                    record.setMaxSimilarity(simBD);
                    record.setMatchedAssetId(asset.getId());
                }

                // 若相似度超过 85% 阈值，提示存在高相似侵权嫌疑
                if (simBD.compareTo(new BigDecimal("85.00")) >= 0) {
                    record.setPassFlag(false);
                    record.setAuditRemark("预审拦截: 检测到与极高度相似的作品二次加工 (相似度 " + simBD + "%, 冲突资产 ID: " + asset.getId() + ")");
                }
            }
        }

        return record;
    }
}
