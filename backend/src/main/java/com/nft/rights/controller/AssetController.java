package com.nft.rights.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nft.rights.entity.AuditRecord;
import com.nft.rights.entity.DigitalAsset;
import com.nft.rights.mapper.AuditRecordMapper;
import com.nft.rights.mapper.DigitalAssetMapper;
import com.nft.rights.service.PreCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数字资产确权存证、预审与查询控制器 (数据持久化至 MySQL 数据库)
 */
@RestController
@RequestMapping("/api/asset")
@CrossOrigin(origins = "*")
public class AssetController {

    @Autowired
    private PreCheckService preCheckService;

    @Autowired
    private DigitalAssetMapper digitalAssetMapper;

    @Autowired
    private AuditRecordMapper auditRecordMapper;

    /**
     * 链下查重预审接口
     */
    @PostMapping("/pre-check")
    public ResponseEntity<AuditRecord> preCheck(@RequestParam("file") MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        List<DigitalAsset> existingList = digitalAssetMapper.selectList(null);
        AuditRecord auditRecord = preCheckService.performPreCheck(bytes, existingList);
        if (auditRecord != null) {
            auditRecordMapper.insert(auditRecord);
        }
        return ResponseEntity.ok(auditRecord);
    }

    /**
     * 模拟 IPFS 上传并保存链下资产预备记录
     */
    @PostMapping("/upload-ipfs")
    public ResponseEntity<JSONObject> uploadToIpfs(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("title") String title,
                                                     @RequestParam("description") String description,
                                                     @RequestParam("creatorWallet") String creatorWallet,
                                                     @RequestParam(value = "royaltyBps", required = false) Integer royaltyBps) throws Exception {
        byte[] bytes = file.getBytes();
        String sha256 = preCheckService.calculateSHA256(bytes);
        String pHash = preCheckService.calculatePerceptualHash(bytes);

        // 模拟 IPFS CID
        String fileCid = "QmSourceFile" + sha256.substring(0, 20);
        String metaCid = "QmMetaJSON" + sha256.substring(0, 20);

        DigitalAsset asset = new DigitalAsset();
        asset.setTitle(title);
        asset.setDescription(description);
        asset.setCategory("ART");
        asset.setFileHash(sha256);
        asset.setPhash(pHash);
        asset.setIpfsCid(fileCid);
        asset.setMetadataCid(metaCid);
        asset.setCreatorWallet(creatorWallet);
        asset.setOwnerWallet(creatorWallet);
        asset.setRoyaltyBps(royaltyBps != null ? royaltyBps : 500);
        asset.setLicenseType("COMMERCIAL");
        asset.setPriceEth(new BigDecimal("0.1"));
        asset.setStatus("AUDITED");
        asset.setCreatedAt(LocalDateTime.now());

        digitalAssetMapper.insert(asset);

        JSONObject res = new JSONObject();
        res.put("assetId", asset.getId());
        res.put("fileHash", sha256);
        res.put("ipfsCid", fileCid);
        res.put("metadataCid", metaCid);
        res.put("royaltyBps", asset.getRoyaltyBps());

        return ResponseEntity.ok(res);
    }

    /**
     * 保存/更新上链铸造完成的数字资产
     */
    @PostMapping("/create")
    public ResponseEntity<DigitalAsset> createAsset(@RequestBody DigitalAsset asset) {
        if (asset.getCreatedAt() == null) {
            asset.setCreatedAt(LocalDateTime.now());
        }
        if (asset.getStatus() == null) {
            asset.setStatus("ON_SALE");
        }
        if (asset.getPriceEth() == null) {
            asset.setPriceEth(new BigDecimal("0.50"));
        }

        if (asset.getId() != null && digitalAssetMapper.selectById(asset.getId()) != null) {
            digitalAssetMapper.updateById(asset);
        } else {
            digitalAssetMapper.insert(asset);
            if (asset.getTokenId() == null) {
                asset.setTokenId(asset.getId());
                digitalAssetMapper.updateById(asset);
            }
        }
        return ResponseEntity.ok(asset);
    }

    /**
     * 资产列表与状态查询
     */
    @GetMapping("/list")
    public ResponseEntity<List<DigitalAsset>> listAssets() {
        return ResponseEntity.ok(digitalAssetMapper.selectList(null));
    }

    /**
     * 更新资产状态（用于智能合约下架后同步链下状态）
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<DigitalAsset> updateAssetStatus(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        DigitalAsset asset = digitalAssetMapper.selectById(id);
        if (asset == null) {
            asset = digitalAssetMapper.selectOne(new QueryWrapper<DigitalAsset>().eq("token_id", id));
        }
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        String newStatus = body.get("status");
        if (newStatus != null && !newStatus.isEmpty()) {
            asset.setStatus(newStatus);
            digitalAssetMapper.updateById(asset);
        }
        return ResponseEntity.ok(asset);
    }

    /**
     * 资产详情查询
     */
    @GetMapping("/{id}")
    public ResponseEntity<DigitalAsset> getAssetDetail(@PathVariable Long id) {
        DigitalAsset asset = digitalAssetMapper.selectById(id);
        if (asset == null) {
            asset = digitalAssetMapper.selectOne(new QueryWrapper<DigitalAsset>().eq("token_id", id));
        }
        return ResponseEntity.ok(asset);
    }
}
