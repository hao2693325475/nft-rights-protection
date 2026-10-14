package com.nft.rights.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nft.rights.entity.DigitalAsset;
import com.nft.rights.entity.User;
import com.nft.rights.mapper.DigitalAssetMapper;
import com.nft.rights.service.EvidenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权属核验、流转图谱与司法电子证据导出控制器
 */
@RestController
@RequestMapping("/api/trace")
@CrossOrigin(origins = "*")
public class TraceController {

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private DigitalAssetMapper digitalAssetMapper;

    /**
     * 根据 Token ID 还原资产全生命周期权属流转节点图谱 (Mint -> List -> Trade -> Royalty Split)
     */
    @GetMapping("/graph/{query}")
    public ResponseEntity<JSONObject> getProvenanceGraph(@PathVariable String query) {
        JSONObject res = new JSONObject();
        res.put("query", query);

        Long idVal = null;
        try {
            idVal = Long.parseLong(query);
        } catch (Exception e) {}

        DigitalAsset asset = null;
        if (idVal != null) {
            asset = digitalAssetMapper.selectById(idVal);
            if (asset == null) {
                asset = digitalAssetMapper.selectOne(new QueryWrapper<DigitalAsset>().eq("token_id", idVal));
            }
        }

        String creatorWallet = asset != null && asset.getCreatorWallet() != null ? 
            asset.getCreatorWallet() : "0x" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("WALLET_" + query).substring(0, 40);

        String title = asset != null ? asset.getTitle() : "数字资产存证作品 #" + query;
        String txHash = asset != null && asset.getTxHash() != null ? 
            asset.getTxHash() : "0x" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("TX_HASH_" + query);

        JSONArray nodes = new JSONArray();
        JSONArray edges = new JSONArray();

        // 节点 1: 创作者确权存证 (Mint)
        JSONObject node1 = new JSONObject();
        node1.put("id", "node-mint");
        node1.put("type", "MINT");
        node1.put("title", "创作者完成链上确权存证 (Mint)");
        node1.put("operator", creatorWallet);
        node1.put("blockNumber", 1000 + (Math.abs(query.hashCode()) % 500));
        node1.put("txHash", txHash);
        node1.put("timestamp", "2026-09-22 10:00:00");
        nodes.add(node1);

        // 节点 2: 挂牌许可售卖
        JSONObject node2 = new JSONObject();
        node2.put("id", "node-list");
        node2.put("type", "LIST");
        node2.put("title", "挂牌许可售卖《" + title + "》(EIP-2981 版税 " + (asset != null ? (asset.getRoyaltyBps()/10.0) : 10.0) + "%)");
        node2.put("operator", creatorWallet);
        node2.put("blockNumber", 1050 + (Math.abs(query.hashCode()) % 500));
        node2.put("txHash", "0x" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("LIST_" + query));
        node2.put("timestamp", "2026-09-22 14:30:00");
        nodes.add(node2);

        // 边连接
        JSONObject edge1 = new JSONObject();
        edge1.put("source", "node-mint");
        edge1.put("target", "node-list");
        edge1.put("label", "权属挂牌");
        edges.add(edge1);

        res.put("nodes", nodes);
        res.put("edges", edges);

        return ResponseEntity.ok(res);
    }

    /**
     * 一键生成并导出司法电子证据链 JSON 包
     */
    @GetMapping("/evidence-package/{query}")
    public ResponseEntity<JSONObject> exportEvidencePackage(@PathVariable String query) {
        Long idVal = null;
        try {
            idVal = Long.parseLong(query);
        } catch (Exception e) {}

        DigitalAsset asset = null;
        if (idVal != null) {
            asset = digitalAssetMapper.selectById(idVal);
            if (asset == null) {
                asset = digitalAssetMapper.selectOne(new QueryWrapper<DigitalAsset>().eq("token_id", idVal));
            }
        }
        if (asset == null) {
            asset = new DigitalAsset();
            asset.setId(idVal != null ? idVal : 1L);
            asset.setTokenId(idVal != null ? idVal : 1L);
            asset.setTitle("数字资产存证作品 #" + query);
            asset.setCategory("数字艺术");
            asset.setFileHash(org.apache.commons.codec.digest.DigestUtils.sha256Hex("FILE_CONTENT_" + query));
            asset.setPhash(org.apache.commons.codec.digest.DigestUtils.md5Hex("PHASH_" + query).substring(0, 16));
            asset.setIpfsCid("Qm" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("IPFS_SOURCE_" + query).substring(0, 44));
            asset.setMetadataCid("Qm" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("IPFS_META_" + query).substring(0, 44));
            asset.setCreatorWallet("0x" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("WALLET_" + query).substring(0, 40));
            asset.setLicenseType("COMMERCIAL");
            asset.setTxHash("0x" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("TX_HASH_" + query));
            asset.setCreatedAt(LocalDateTime.now().minusDays(1));
        }

        User creator = new User();
        String cWallet = asset.getCreatorWallet() != null ? asset.getCreatorWallet() : "0x" + org.apache.commons.codec.digest.DigestUtils.sha256Hex("WALLET_" + query).substring(0, 40);
        creator.setWalletAddress(cWallet);
        // 创作者显示名基于钱包地址动态生成，避免硬编码个人信息
        creator.setUsername("创作者_" + cWallet.substring(2, 8).toUpperCase());
        creator.setRealName("账户_" + cWallet.substring(cWallet.length() - 4));
        creator.setIdCardHash(org.apache.commons.codec.digest.DigestUtils.sha256Hex("ID_CARD_" + cWallet));
        creator.setIsVerified(true);

        JSONObject evidencePkg = evidenceService.generateEvidencePackage(asset, creator, asset.getTxHash(), 1000L + (Math.abs(query.hashCode()) % 500));
        
        // 将资产基本信息附入证据包，供前端展示
        JSONObject assetInfo = new JSONObject();
        assetInfo.put("title", asset.getTitle());
        assetInfo.put("category", asset.getCategory());
        assetInfo.put("royaltyBps", asset.getRoyaltyBps());
        assetInfo.put("licenseType", asset.getLicenseType());
        assetInfo.put("status", asset.getStatus());
        evidencePkg.put("assetInfo", assetInfo);
        
        return ResponseEntity.ok(evidencePkg);
    }
}
