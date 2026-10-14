# 基于区块链的NFT数字资产权益保护系统 - 系统代码

本工程为毕业论文 **《基于区块链的NFT数字资产权益保护系统设计与实现》** 的配套完整实现代码。系统基于 B/S 架构与 DApp 分层架构开发，实现了链下查重复审与链上存证协同、IPFS 双重存储与元数据安全设计、EIP-2981 版税自动分成、全生命周期权属流转图谱与司法电子证据链等核心机制。

---

## 📁 目录结构说明

```
系统代码/
├── contracts/               # 智能合约工程 (Hardhat + Solidity 0.8.19)
│   ├── contracts/
│   │   ├── NFTAsset.sol            # ERC-721 链上确权存证与防重铸核心合约
│   │   ├── RoyaltyManager.sol      # EIP-2981 版税收益自动分成与结算合约
│   │   └── AccessController.sol    # 角色权限管理与违规资产下架控制合约
│   ├── scripts/
│   │   └── deploy.js               # 本地/测试网一键部署脚本
│   ├── test/
│   │   └── NFTAsset.test.js        # 智能合约安全性与功能单元测试套件
│   ├── hardhat.config.js
│   └── package.json
│
├── backend/                 # 后端辅助业务服务 (Spring Boot 3.x)
│   ├── src/main/java/com/nft/rights/
│   │   ├── controller/             # Asset, Trade, Trace, Auth, Admin API 接口
│   │   ├── service/                # PreCheck (SHA-256/pHash), Evidence (司法电子证据), Web3Auth
│   │   └── entity/                 # User, DigitalAsset, AuditRecord 等数据库实体
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── frontend/                # 去中心化前端 DApp (Vue 3 + Vite + TailwindCSS + Ethers.js)
│   ├── src/
│   │   ├── views/                  # HomeView, MintView, MarketView, TraceView, AdminView
│   │   ├── store/                  # Pinia Web3 MetaMask 状态管理
│   │   ├── router/                 # Vue Router 路由
│   │   └── style.css               # 玻璃拟态炫彩科技风 CSS 样式系统
│   ├── package.json
│   └── vite.config.js
│
└── database/                # 数据库脚本
    └── schema.sql                  # MySQL 8.0 建表脚本及种子数据
```

---

## 🚀 快速启动指南

### 1. 数据库初始化 (MySQL 8.0+)
1. 创建数据库并导入 `database/schema.sql` 脚本：
   ```bash
   mysql -u root -p < database/schema.sql
   ```

### 2. 智能合约编译与 Sepolia 测试网 / 本地节点部署 (Hardhat)
```bash
cd contracts

# 编译智能合约
npx hardhat compile

# 执行智能合约安全性与功能测试套件
npx hardhat test

# 选项 A: 部署合约至 Sepolia 测试网 (推荐)
# 需要在 contracts/.env 中配置 PRIVATE_KEY 与 SEPOLIA_RPC_URL
npm run deploy:sepolia

# 选项 B: 本地开发部署 (启动本地节点并部署)
# npx hardhat node
# npm run deploy:local
```

### 3. 启动后端服务 (Spring Boot)
```bash
cd backend
mvn clean spring-boot:run
```
后端 RESTful API 将运行在 `http://localhost:8080`。

### 4. 启动前端 Web3 DApp (Vue 3)
```bash
cd frontend
npm install
npm run dev
```
在浏览器中打开 `http://localhost:3000`，配合 MetaMask 插件交互。

---

## 🛠️ 核心功能模块清单

1. **链下查重预审模块**：
   - SHA-256 源文件指纹比对 (100% 碰撞重铸拦截)
   - 感知哈希 (pHash) 图像相似度汉明距离检测
2. **IPFS 加密与 Metadata 存储**：
   - 符合 ERC-721 Metadata 标准 JSON 生成与固定
3. **EIP-2981 版税自动分成**：
   - 二级市场转售收益强制扣留并暂存给原创创作者 (Pull-over-Push 提现)
4. **全生命周期溯源与司法证据链**：
   - Event Log 时序还原权属节点图谱
   - 一键生成与导出包含链上证明与时间戳的 JSON 电子证据包
