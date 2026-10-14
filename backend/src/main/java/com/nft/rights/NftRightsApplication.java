package com.nft.rights;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nft.rights.mapper")
public class NftRightsApplication {
    public static void main(String[] args) {
        SpringApplication.run(NftRightsApplication.class, args);
        System.out.println("==================================================");
        System.out.println("基于区块链的NFT数字资产权益保护系统 Backend Running!");
        System.out.println("==================================================");
    }
}
