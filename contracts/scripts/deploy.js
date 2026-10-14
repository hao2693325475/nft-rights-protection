const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("==========================================");
  console.log("部署基于区块链的NFT数字资产权益保护系统智能合约");
  console.log("==========================================");

  const [deployer] = await hre.ethers.getSigners();
  console.log("部署账户:", deployer.address);

  // 1. 部署 RoyaltyManager
  const RoyaltyManager = await hre.ethers.getContractFactory("RoyaltyManager");
  const royaltyManager = await RoyaltyManager.deploy();
  await royaltyManager.waitForDeployment();
  const royaltyManagerAddress = await royaltyManager.getAddress();
  console.log("RoyaltyManager 部署成功，合约地址:", royaltyManagerAddress);

  await new Promise(r => setTimeout(r, 2000));

  // 2. 部署 AccessController
  const AccessController = await hre.ethers.getContractFactory("AccessController");
  const accessController = await AccessController.deploy();
  await accessController.waitForDeployment();
  const accessControllerAddress = await accessController.getAddress();
  console.log("AccessController 部署成功，合约地址:", accessControllerAddress);

  await new Promise(r => setTimeout(r, 2000));

  // 3. 部署核心 NFTAsset 合约
  const NFTAsset = await hre.ethers.getContractFactory("NFTAsset");
  const nftAsset = await NFTAsset.deploy(royaltyManagerAddress, accessControllerAddress);
  await nftAsset.waitForDeployment();
  const nftAssetAddress = await nftAsset.getAddress();
  console.log("NFTAsset 核心确权合约部署成功，地址:", nftAssetAddress);

  // 4. 将 RoyaltyManager 的 owner 转移给 NFTAsset 合约或配置权限
  await royaltyManager.transferOwnership(nftAssetAddress);
  console.log("RoyaltyManager 所有权转移至 NFTAsset");

  // 保存部署地址输出到 JSON
  const contractsConfig = {
    network: hre.network.name,
    chainId: (await hre.ethers.provider.getNetwork()).chainId.toString(),
    RoyaltyManager: royaltyManagerAddress,
    AccessController: accessControllerAddress,
    NFTAsset: nftAssetAddress,
    deployedAt: new Date().toISOString()
  };

  const outputPath = path.join(__dirname, "../deployed-contracts.json");
  fs.writeFileSync(outputPath, JSON.stringify(contractsConfig, null, 2));
  console.log("1. 合约部署配置信息已保存至:", outputPath);

  // 智能同步写回后端 application.yml
  try {
    const ymlPath = path.join(__dirname, "../../backend/src/main/resources/application.yml");
    if (fs.existsSync(ymlPath)) {
      let ymlContent = fs.readFileSync(ymlPath, "utf-8");
      ymlContent = ymlContent.replace(/nft-asset:\s*".*?"/, `nft-asset: "${nftAssetAddress}"`);
      ymlContent = ymlContent.replace(/royalty-manager:\s*".*?"/, `royalty-manager: "${royaltyManagerAddress}"`);
      ymlContent = ymlContent.replace(/access-controller:\s*".*?"/, `access-controller: "${accessControllerAddress}"`);
      fs.writeFileSync(ymlPath, ymlContent, "utf-8");
      console.log("2. 已自动同步最新的合约地址至后端: application.yml");
    }
  } catch (e) {
    console.warn("同步后端配置失败:", e.message);
  }

  // 智能同步写回前端 wallet.js
  try {
    const walletStorePath = path.join(__dirname, "../../frontend/src/store/wallet.js");
    if (fs.existsSync(walletStorePath)) {
      let walletContent = fs.readFileSync(walletStorePath, "utf-8");
      walletContent = walletContent.replace(/nftAssetContractAddress:\s*'.*?'/, `nftAssetContractAddress: '${nftAssetAddress}'`);
      walletContent = walletContent.replace(/royaltyManagerContractAddress:\s*'.*?'/, `royaltyManagerContractAddress: '${royaltyManagerAddress}'`);
      walletContent = walletContent.replace(/accessControllerContractAddress:\s*'.*?'/, `accessControllerContractAddress: '${accessControllerAddress}'`);
      fs.writeFileSync(walletStorePath, walletContent, "utf-8");
      console.log("3. 已自动同步最新的合约地址至前端: wallet.js");
    }
  } catch (e) {
    console.warn("同步前端配置失败:", e.message);
  }

  console.log("==========================================");
}

main().catch((error) => {
  console.error("部署异常失败:", error);
  process.exitCode = 1;
});
