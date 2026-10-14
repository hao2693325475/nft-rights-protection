const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("NFT数字资产权益保护系统 - 智能合约测试", function () {
  let RoyaltyManager, royaltyManager;
  let AccessController, accessController;
  let NFTAsset, nftAsset;
  let owner, creator, buyer, infringer;

  const sampleFileHash = ethers.keccak256(ethers.toUtf8Bytes("Sample Digital Art Source File Content 2027"));
  const sampleIpfsCID = "QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco";
  const sampleMetadataCID = "QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG";
  const royaltyBps = 1000; // 10%

  beforeEach(async function () {
    [owner, creator, buyer, infringer] = await ethers.getSigners();

    RoyaltyManager = await ethers.getContractFactory("RoyaltyManager");
    royaltyManager = await RoyaltyManager.deploy();
    await royaltyManager.waitForDeployment();

    AccessController = await ethers.getContractFactory("AccessController");
    accessController = await AccessController.deploy();
    await accessController.waitForDeployment();

    NFTAsset = await ethers.getContractFactory("NFTAsset");
    nftAsset = await NFTAsset.deploy(await royaltyManager.getAddress(), await accessController.getAddress());
    await nftAsset.waitForDeployment();

    // 将 royaltyManager 所有权转给 nftAsset
    await royaltyManager.transferOwnership(await nftAsset.getAddress());
  });

  describe("1. 资产确权与防重复铸造测试", function () {
    it("创作者应能成功铸造数字资产并写入链上存证凭证", async function () {
      await expect(
        nftAsset.connect(creator).mintAsset(
          creator.address,
          sampleIpfsCID,
          sampleMetadataCID,
          sampleFileHash,
          royaltyBps
        )
      ).to.emit(nftAsset, "AssetMinted");

      expect(await nftAsset.ownerOf(1)).to.equal(creator.address);
      
      const proof = await nftAsset.assetProofs(1);
      expect(proof.fileHash).to.equal(sampleFileHash);
      expect(proof.creator).to.equal(creator.address);
      expect(proof.ipfsCID).to.equal(sampleIpfsCID);
    });

    it("同一作品哈希禁止重复上链铸造 (防止抢注侵权)", async function () {
      await nftAsset.connect(creator).mintAsset(
        creator.address,
        sampleIpfsCID,
        sampleMetadataCID,
        sampleFileHash,
        royaltyBps
      );

      await expect(
        nftAsset.connect(infringer).mintAsset(
          infringer.address,
          sampleIpfsCID,
          sampleMetadataCID,
          sampleFileHash,
          royaltyBps
        )
      ).to.be.revertedWith("NFTAsset: Asset hash already registered on-chain");
    });
  });

  describe("2. EIP-2981 版税分成与自动结算测试", function () {
    beforeEach(async function () {
      await nftAsset.connect(creator).mintAsset(
        creator.address,
        sampleIpfsCID,
        sampleMetadataCID,
        sampleFileHash,
        royaltyBps
      );
    });

    it("二级市场交易时应自动结算 10% 版税给创作者", async function () {
      const price = ethers.parseEther("1.0");

      // 创作者挂牌售卖
      await nftAsset.connect(creator).listAsset(1, price);

      // 买家购买资产
      await expect(
        nftAsset.connect(buyer).buyAsset(1, { value: price })
      ).to.emit(nftAsset, "AssetPurchased");

      // 验证所有权转移给买家
      expect(await nftAsset.ownerOf(1)).to.equal(buyer.address);

      // 验证创作者在 RoyaltyManager 中产生暂存版税 (1.0 ETH 的 10% = 0.1 ETH)
      const expectedRoyalty = ethers.parseEther("0.1");
      expect(await royaltyManager.pendingBalances(creator.address)).to.equal(expectedRoyalty);

      // 创作者提取版税
      const initialBalance = await ethers.provider.getBalance(creator.address);
      await royaltyManager.connect(creator).withdrawRoyalty();
      const finalBalance = await ethers.provider.getBalance(creator.address);

      expect(finalBalance).to.be.gt(initialBalance);
    });
  });

  describe("3. 侵权黑名单下架测试", function () {
    it("管理员应能将侵权资产拉黑，拉黑后禁止挂牌交易", async function () {
      await nftAsset.connect(creator).mintAsset(
        creator.address,
        sampleIpfsCID,
        sampleMetadataCID,
        sampleFileHash,
        royaltyBps
      );

      // 管理员下架 Token 1
      await accessController.blacklistAsset(1, "检测到盗用版权照片");
      expect(await accessController.isAssetActive(1)).to.be.false;

      // 尝试挂牌交易应回滚失败
      await expect(
        nftAsset.connect(creator).listAsset(1, ethers.parseEther("0.5"))
      ).to.be.revertedWith("NFTAsset: Asset is blacklisted");
    });
  });
});
