package com.nft.rights.service;

import org.springframework.stereotype.Service;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Web3 去中心化钱包 MetaMask 身份认证
 * 验证前端传递的随机 Message 签名，使用 ECRecover 还原以太坊钱包公钥与地址
 */
@Service
public class Web3AuthService {

    private static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    public boolean verifySignature(String message, String signature, String expectedWalletAddress) {
        try {
            // 构造 Ethereum 带有前缀的消息哈希
            String prefix = PERSONAL_MESSAGE_PREFIX + message.length();
            byte[] msgBytes = (prefix + message).getBytes();
            byte[] msgHash = Hash.sha3(msgBytes);

            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            byte v = signatureBytes[64];
            if (v < 27) {
                v += 27;
            }

            Sign.SignatureData signatureData = new Sign.SignatureData(
                    v,
                    Arrays.copyOfRange(signatureBytes, 0, 32),
                    Arrays.copyOfRange(signatureBytes, 32, 64)
            );

            BigInteger pubKey = Sign.signedMessageHashToKey(msgHash, signatureData);
            String recoveredAddress = "0x" + Keys.getAddress(pubKey);

            return recoveredAddress.equalsIgnoreCase(expectedWalletAddress);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
