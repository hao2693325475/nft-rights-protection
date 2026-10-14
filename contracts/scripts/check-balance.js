const hre = require("hardhat");

async function main() {
  const addr = "0xE38b90a2aa71da5390c95c14a69bb3c13d2ae3f6";
  const formattedAddress = hre.ethers.getAddress(addr.toLowerCase());
  const balance = await hre.ethers.provider.getBalance(formattedAddress);
  console.log(`Node balance for ${formattedAddress}: ${hre.ethers.formatEther(balance)} ETH`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
