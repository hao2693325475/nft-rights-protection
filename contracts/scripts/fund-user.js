const hre = require("hardhat");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("正在使用 Hardhat 节点主账户 (" + deployer.address + ") 为您的所有 MetaMask 账户批量转账测试 ETH...");

  const rawAddresses = [
    "0xE38b90A2Aa71DA5390c95c14a69bB3C13D2aE3f6", // Account 1
    "0x6680459524021703A0b34336B579c162624f2146", // Account 2
    "0xB8Bccb8d00B1a24dEE65e2365313d463A297817a", // Account 3
    "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266", // Account 4
    "0x71bE63f3384f5fb98995898A86B02Fb2426c5788", // Account 5
    "0xfcBAb0B607142174001190645c1964610713e629"  // Account 6
  ];

  for (let i = 0; i < rawAddresses.length; i++) {
    const addr = rawAddresses[i];
    try {
      const formattedAddress = hre.ethers.getAddress(addr.toLowerCase());
      const tx = await deployer.sendTransaction({
        to: formattedAddress,
        value: hre.ethers.parseEther("100.0")
      });
      await tx.wait();
      console.log(`✅ [Account ${i + 1}] 成功向 [ ${formattedAddress} ] 划拨 100.00 ETH！Tx: ${tx.hash}`);
    } catch (err) {
      console.error(`❌ [Account ${i + 1}] 向 [ ${addr} ] 转账失败:`, err.message);
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
