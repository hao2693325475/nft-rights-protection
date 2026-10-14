// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/token/ERC721/extensions/ERC721Enumerable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/security/ReentrancyGuard.sol";
import "./RoyaltyManager.sol";
import "./AccessController.sol";

/**
 * @title NFTAsset
 * @dev 基于 ERC-721 及 EIP-2981 的数字资产权益确权与版权防篡改核心智能合约
 */
contract NFTAsset is ERC721Enumerable, Ownable, ReentrancyGuard {
    uint256 private _nextTokenId = 1;

    // 结构体: 链上确权凭证与证据数据
    struct RightsProof {
        bytes32 fileHash;       // 作品源文件 SHA-256 哈希
        string ipfsCID;         // 原文件 IPFS 节点内容标识符
        string metadataCID;     // ERC-721 Metadata JSON CID
        address creator;        // 原创创作者地址
        uint256 mintTimestamp; // 链上落块存证时间戳
        uint256 priceEth;       // 挂牌价格 (Wei)
        bool isOnSale;          // 是否在售
    }

    // tokenId => RightsProof 映射
    mapping(uint256 => RightsProof) public assetProofs;

    // fileHash => tokenId 映射 (用于链上源头防二次重铸/抢代存证)
    mapping(bytes32 => uint256) private _hashToTokenId;

    // 关联外部合约
    RoyaltyManager public royaltyManager;
    AccessController public accessController;

    // 事件定义 (用于全生命周期流转图谱构建与追溯)
    event AssetMinted(
        uint256 indexed tokenId,
        address indexed creator,
        bytes32 indexed fileHash,
        string ipfsCID,
        string metadataCID,
        uint256 timestamp
    );
    event AssetListed(uint256 indexed tokenId, uint256 price);
    event AssetUnlisted(uint256 indexed tokenId);
    event AssetPurchased(
        uint256 indexed tokenId,
        address indexed seller,
        address indexed buyer,
        uint256 price,
        uint256 royaltyPaid
    );

    constructor(address _royaltyManager, address _accessController) ERC721("NFT Digital Rights Asset", "NDRA") {
        royaltyManager = RoyaltyManager(_royaltyManager);
        accessController = AccessController(_accessController);
    }

    /**
     * @dev 数字资产链上确权登记与 NFT 铸造方法 (防止碰撞与抢注)
     * @param to 接收 Token 钱包地址
     * @param ipfsCID 资产原文件 IPFS CID
     * @param metadataCID Metadata JSON CID
     * @param fileHash 文件 SHA-256 指纹
     * @param royaltyFraction 版税比例 (bps, 如 500 = 5%)
     */
    function mintAsset(
        address to,
        string calldata ipfsCID,
        string calldata metadataCID,
        bytes32 fileHash,
        uint96 royaltyFraction
    ) external nonReentrant returns (uint256) {
        require(_hashToTokenId[fileHash] == 0, "NFTAsset: Asset hash already registered on-chain");
        require(bytes(ipfsCID).length > 0, "NFTAsset: Empty IPFS CID");
        require(bytes(metadataCID).length > 0, "NFTAsset: Empty Metadata CID");

        uint256 newTokenId = _nextTokenId++;
        
        _safeMint(to, newTokenId);
        
        assetProofs[newTokenId] = RightsProof({
            fileHash: fileHash,
            ipfsCID: ipfsCID,
            metadataCID: metadataCID,
            creator: msg.sender,
            mintTimestamp: block.timestamp,
            priceEth: 0,
            isOnSale: false
        });

        _hashToTokenId[fileHash] = newTokenId;

        // 向 RoyaltyManager 登记版税
        royaltyManager.setTokenRoyalty(newTokenId, msg.sender, royaltyFraction);

        emit AssetMinted(newTokenId, msg.sender, fileHash, ipfsCID, metadataCID, block.timestamp);

        return newTokenId;
    }

    /**
     * @dev 将资产挂牌出售
     */
    function listAsset(uint256 tokenId, uint256 priceEth) external {
        require(ownerOf(tokenId) == msg.sender, "NFTAsset: Only token owner can list");
        require(accessController.isAssetActive(tokenId), "NFTAsset: Asset is blacklisted");
        require(priceEth > 0, "NFTAsset: Price must be greater than 0");

        assetProofs[tokenId].priceEth = priceEth;
        assetProofs[tokenId].isOnSale = true;

        emit AssetListed(tokenId, priceEth);
    }

    /**
     * @dev 购买资产并自动扣结算 EIP-2981 版税分成
     */
    function buyAsset(uint256 tokenId) external payable nonReentrant {
        RightsProof storage proof = assetProofs[tokenId];
        require(proof.isOnSale, "NFTAsset: Asset not on sale");
        require(msg.value >= proof.priceEth, "NFTAsset: Insufficient ETH sent");
        require(accessController.isAssetActive(tokenId), "NFTAsset: Asset is blacklisted");

        address seller = ownerOf(tokenId);
        require(seller != msg.sender, "NFTAsset: Cannot buy your own asset");

        // 重置在售状态
        proof.isOnSale = false;
        uint256 salePrice = msg.value;

        // 计算 EIP-2981 版税
        (address royaltyReceiver, uint256 royaltyAmount) = royaltyManager.royaltyInfo(tokenId, salePrice);

        uint256 sellerAmount = salePrice;

        // 若存在版税且接收者有效，扣除版税并存入 RoyaltyManager
        if (royaltyReceiver != address(0) && royaltyAmount > 0 && royaltyAmount < salePrice) {
            sellerAmount = salePrice - royaltyAmount;
            royaltyManager.depositRoyalty{value: royaltyAmount}(tokenId);
        }

        // 卖家获得剩余净收益
        (bool sellerTxSuccess, ) = payable(seller).call{value: sellerAmount}("");
        require(sellerTxSuccess, "NFTAsset: Seller transfer failed");

        // 转移 NFT 所有权
        _transfer(seller, msg.sender, tokenId);

        emit AssetPurchased(tokenId, seller, msg.sender, salePrice, royaltyAmount);
    }

    /**
     * @dev 查询作品哈希对应的 Token ID (验真接口)
     */
    function getTokenIdByHash(bytes32 fileHash) external view returns (uint256) {
        return _hashToTokenId[fileHash];
    }

    /**
     * @dev 重写 tokenURI，指向 IPFS Metadata CID
     */
    function tokenURI(uint256 tokenId) public view override returns (string memory) {
        require(_exists(tokenId), "NFTAsset: URI query for nonexistent token");
        return string(abi.encodePacked("ipfs://", assetProofs[tokenId].metadataCID));
    }
}
