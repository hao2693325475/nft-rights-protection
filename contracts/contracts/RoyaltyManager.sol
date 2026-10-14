// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/interfaces/IERC2981.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/security/ReentrancyGuard.sol";
import "@openzeppelin/contracts/utils/introspection/ERC165.sol";

/**
 * @title RoyaltyManager
 * @dev 版税收益自动分成与结算智能合约 (EIP-2981 标准实现)
 */
contract RoyaltyManager is IERC2981, ERC165, Ownable, ReentrancyGuard {
    struct RoyaltyInfo {
        address recipient; // 版税收益接收地址(原创作者)
        uint96 royaltyFraction; // 版税比例，以基点计算 (例: 500 = 5.00%, 10000 = 100%)
    }

    // tokenId => RoyaltyInfo 映射
    mapping(uint256 => RoyaltyInfo) private _tokenRoyalties;
    
    // 创作者累计可提取版税余额 (Pull-over-Push 模式)
    mapping(address => uint256) public pendingBalances;

    // 最大版税限制为 20% (2000 bps)
    uint96 public constant MAX_ROYALTY_BPS = 2000;

    event RoyaltySet(uint256 indexed tokenId, address indexed recipient, uint96 value);
    event RoyaltyReceived(uint256 indexed tokenId, address indexed creator, uint256 amount);
    event RoyaltyWithdrawn(address indexed creator, uint256 amount);

    /**
     * @dev 为指定 Token ID 设置版税接收人与比例
     */
    function setTokenRoyalty(uint256 tokenId, address recipient, uint96 feeNumerator) external onlyOwner {
        require(feeNumerator <= MAX_ROYALTY_BPS, "RoyaltyManager: Royalty fee exceeds maximum limit (20%)");
        require(recipient != address(0), "RoyaltyManager: Invalid recipient address");
        _tokenRoyalties[tokenId] = RoyaltyInfo(recipient, feeNumerator);
        emit RoyaltySet(tokenId, recipient, feeNumerator);
    }

    /**
     * @dev EIP-2981 标准接口实现: 查询交易金额下应归属创作者的版税金额
     */
    function royaltyInfo(uint256 tokenId, uint256 salePrice) external view override returns (address receiver, uint256 royaltyAmount) {
        RoyaltyInfo memory royalty = _tokenRoyalties[tokenId];
        if (royalty.recipient == address(0)) {
            return (address(0), 0);
        }
        uint256 amount = (salePrice * royalty.royaltyFraction) / 10000;
        return (royalty.recipient, amount);
    }

    /**
     * @dev 结算版税并暂存至创作者余额 (避免链上直接转账导致DOS攻击)
     */
    function depositRoyalty(uint256 tokenId) external payable nonReentrant {
        RoyaltyInfo memory royalty = _tokenRoyalties[tokenId];
        require(royalty.recipient != address(0), "RoyaltyManager: No royalty set for token");
        require(msg.value > 0, "RoyaltyManager: Deposit value must be greater than 0");

        pendingBalances[royalty.recipient] += msg.value;
        emit RoyaltyReceived(tokenId, royalty.recipient, msg.value);
    }

    /**
     * @dev 创作者主动提取累计版税收益
     */
    function withdrawRoyalty() external nonReentrant {
        uint256 amount = pendingBalances[msg.sender];
        require(amount > 0, "RoyaltyManager: No pending balance to withdraw");

        pendingBalances[msg.sender] = 0;
        (bool success, ) = payable(msg.sender).call{value: amount}("");
        require(success, "RoyaltyManager: ETH transfer failed");

        emit RoyaltyWithdrawn(msg.sender, amount);
    }

    /**
     * @dev ERC165 接口查询支持方法
     */
    function supportsInterface(bytes4 interfaceId) public view virtual override(ERC165, IERC165) returns (bool) {
        return interfaceId == type(IERC2981).interfaceId || super.supportsInterface(interfaceId);
    }
}
