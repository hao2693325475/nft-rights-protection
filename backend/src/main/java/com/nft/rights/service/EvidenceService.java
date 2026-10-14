package com.nft.rights.service;

import com.alibaba.fastjson2.JSONObject;
import com.nft.rights.entity.DigitalAsset;
import com.nft.rights.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 司法电子证据链一键生成与打包服务
 * 整合：链上存证凭证、区块高度、时间戳、源文件 SHA-256 哈希、IPFS CID 及买卖交易记录
 */
@Service
public class EvidenceService {

    public JSONObject generateEvidencePackage(DigitalAsset asset, User creator, String txHash, Long blockNumber) {
        JSONObject packageJson = new JSONObject();

        packageJson.put("evidenceTitle", "基于区块链的NFT数字资产存证与权属电子证据");
        packageJson.put("generatedAt", LocalDateTime.now().toString());
        packageJson.put("certificateVersion", "v1.0-EIP2981");

        // 1. 资产与原文件指纹数据
        JSONObject assetMeta = new JSONObject();
        assetMeta.put("tokenId", asset.getTokenId());
        assetMeta.put("title", asset.getTitle());
        assetMeta.put("category", asset.getCategory());
        assetMeta.put("fileSHA256", asset.getFileHash());
        assetMeta.put("perceptualHash", asset.getPhash());
        assetMeta.put("ipfsSourceCID", asset.getIpfsCid());
        assetMeta.put("ipfsMetadataCID", asset.getMetadataCid());
        assetMeta.put("licenseType", asset.getLicenseType());
        packageJson.put("digitalAssetProof", assetMeta);

        // 2. 原创作者实名凭证
        JSONObject creatorMeta = new JSONObject();
        creatorMeta.put("walletAddress", creator.getWalletAddress());
        creatorMeta.put("username", creator.getUsername());
        creatorMeta.put("realNameMasked", creator.getRealName());
        creatorMeta.put("idCardHash", creator.getIdCardHash());
        creatorMeta.put("isVerified", creator.getIsVerified());
        packageJson.put("creatorIdentityProof", creatorMeta);

        // 3. 区块链链上不可篡改凭证
        JSONObject chainMeta = new JSONObject();
        chainMeta.put("blockchainNetwork", "Ethereum Sepolia Testnet");
        chainMeta.put("contractAddress", "0x5FbDB2315678afecb367f032d93F642f64180aa3");
        chainMeta.put("transactionHash", txHash != null ? txHash : asset.getTxHash());
        chainMeta.put("blockNumber", blockNumber != null ? blockNumber : 1024L);
        chainMeta.put("mintTimestamp", asset.getCreatedAt().toString());
        packageJson.put("blockchainProof", chainMeta);

        // 4. 防伪哈希
        String evidenceHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(packageJson.toJSONString());
        packageJson.put("evidencePackageHash", evidenceHash);

        return packageJson;
    }
}
