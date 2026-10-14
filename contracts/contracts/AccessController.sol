// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/access/AccessControl.sol";

/**
 * @title AccessController
 * @dev 系统的角色权限管理与版权授权条款与违规黑名单控制合约
 */
contract AccessController is AccessControl {
    bytes32 public constant ADMIN_ROLE = keccak256("ADMIN_ROLE");
    bytes32 public constant CREATOR_ROLE = keccak256("CREATOR_ROLE");

    // 结构体: 链上授权条款记录
    struct LicenseTerms {
        string licenseType; // 例: "VIEW_ONLY", "COMMERCIAL", "EXCLUSIVE"
        uint256 validUntil; // 授权有效期 (0 为永久授权)
        bool allowSublicensing; // 是否允许二次转授权
    }

    // tokenId => LicenseTerms 映射
    mapping(uint256 => LicenseTerms) public tokenLicenses;

    // tokenId => 是否下架黑名单
    mapping(uint256 => bool) public blacklistedTokens;

    event LicenseUpdated(uint256 indexed tokenId, string licenseType, uint256 validUntil, bool allowSublicensing);
    event AssetBlacklisted(uint256 indexed tokenId, string reason);
    event AssetUnblacklisted(uint256 indexed tokenId);

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(ADMIN_ROLE, msg.sender);
    }

    /**
     * @dev 为 NFT 设置版权授权条款
     */
    function setLicenseTerms(
        uint256 tokenId,
        string calldata licenseType,
        uint256 validUntil,
        bool allowSublicensing
    ) external {
        tokenLicenses[tokenId] = LicenseTerms(licenseType, validUntil, allowSublicensing);
        emit LicenseUpdated(tokenId, licenseType, validUntil, allowSublicensing);
    }

    /**
     * @dev 管理员标记违规侵权资产下架 (入黑名单)
     */
    function blacklistAsset(uint256 tokenId, string calldata reason) external onlyRole(ADMIN_ROLE) {
        blacklistedTokens[tokenId] = true;
        emit AssetBlacklisted(tokenId, reason);
    }

    /**
     * @dev 管理员解除下架
     */
    function unblacklistAsset(uint256 tokenId) external onlyRole(ADMIN_ROLE) {
        blacklistedTokens[tokenId] = false;
        emit AssetUnblacklisted(tokenId);
    }

    /**
     * @dev 查询资产是否可用 (未下架)
     */
    function isAssetActive(uint256 tokenId) external view returns (bool) {
        return !blacklistedTokens[tokenId];
    }
}
