import { defineStore } from 'pinia'
import { BrowserProvider, JsonRpcProvider, Wallet, formatEther, parseEther, getAddress } from 'ethers'

// Hardhat 本地节点默认主账户私钥 (仅用于开发环境测试水龙头充值)
const HARDHAT_FAUCET_PRIVATE_KEY = '0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80'

export const useWalletStore = defineStore('wallet', {
  state: () => ({
    isConnected: false,
    address: '',
    shortAddress: '',
    chainId: null,
    chainName: '',
    balance: '0.0000',
    role: 'CREATOR', // CREATOR, PURCHASER, ADMIN
    isFunding: false,
    nftAssetContractAddress: '0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0',
    royaltyManagerContractAddress: '0x5FbDB2315678afecb367f032d93F642f64180aa3',
    accessControllerContractAddress: '0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512'
  }),

  actions: {
    /**
     * 连接真实 MetaMask 钱包
     */
    async connectWallet() {
      if (typeof window.ethereum === 'undefined') {
        alert('未检测到真实 MetaMask 钱包插件！\n\n请先在 Chrome / Edge 浏览器中安装 MetaMask 扩展。')
        window.open('https://metamask.io/download/', '_blank')
        return false
      }

      try {
        const provider = new BrowserProvider(window.ethereum)
        
        // 唤起 MetaMask 请求用户授权连接账户
        const accounts = await provider.send('eth_requestAccounts', [])
        
        if (!accounts || accounts.length === 0) {
          throw new Error('未在 MetaMask 中选择任何账户')
        }

        const address = accounts[0]
        this.address = address
        this.shortAddress = `${address.substring(0, 6)}...${address.substring(address.length - 4)}`
        this.isConnected = true

        // 读取 network chainId
        try {
          const network = await provider.getNetwork()
          this.chainId = Number(network.chainId)
          this.chainName = network.name === 'unknown' ? `Chain ${network.chainId}` : network.name
        } catch (netErr) {
          console.warn('获取 ChainID 失败:', netErr)
        }

        // 读取当前账户真实余额
        this.balance = await this.fetchBalance(address)

        // 监听 MetaMask 切换账户事件
        if (window.ethereum.removeAllListeners) {
          window.ethereum.removeAllListeners('accountsChanged')
          window.ethereum.removeAllListeners('chainChanged')
        }

        window.ethereum.on('accountsChanged', async (newAccounts) => {
          if (!newAccounts || newAccounts.length === 0) {
            this.disconnectWallet()
          } else {
            // 保持切换后的全新地址，绝不倒退覆盖
            this.address = newAccounts[0]
            this.shortAddress = `${this.address.substring(0, 6)}...${this.address.substring(this.address.length - 4)}`
            this.balance = await this.fetchBalance(this.address)
          }
        })

        // 监听 MetaMask 切换网络
        window.ethereum.on('chainChanged', () => {
          this.refreshWalletData()
        })

        // 默认自动引导连接至 Hardhat 本地测试网 (31337)
        if (this.chainId !== 31337 && this.chainId !== 11155111) {
          this.switchToHardhatLocalhost()
        }

        return true
      } catch (error) {
        console.error('MetaMask 钱包连接过程异常:', error)
        if (error.code !== 4001) {
          alert(`连接账户异常: ${error.message || '请检查 MetaMask 当前选择的网络'}`)
        }
        return false
      }
    },

    /**
     * 重新唤起 MetaMask 账户选择器对话框，允许用户自由勾选切换连接哪一个账号
     */
    async requestChangeAccount() {
      if (typeof window.ethereum === 'undefined') return
      try {
        await window.ethereum.request({
          method: 'wallet_requestPermissions',
          params: [{ eth_accounts: {} }]
        })
        const provider = new BrowserProvider(window.ethereum)
        const accounts = await provider.send('eth_accounts', [])
        if (accounts && accounts.length > 0) {
          this.address = accounts[0]
          this.shortAddress = `${this.address.substring(0, 6)}...${this.address.substring(this.address.length - 4)}`
          this.balance = await this.fetchBalance(this.address)
        }
      } catch (err) {
        console.warn('用户取消了账户选择切换:', err)
      }
    },

    /**
     * 快捷领水 (支持 Sepolia 测试网与 Hardhat 本地测试网)
     */
    async requestFaucetETH() {
      if (!this.isConnected || !this.address) {
        alert('请先连接 MetaMask 钱包！')
        return false
      }

      // 如果是 Sepolia 测试网，引导前往公共水龙头获取 SepoliaETH
      if (this.chainId === 11155111) {
        alert('🎉 Sepolia 测试网领水提示：\n\n已为您打开 Sepolia 水龙头页面，请输入您的钱包地址领取免费测试 ETH。')
        window.open('https://cloud.google.com/application/web3/faucet/ethereum/sepolia', '_blank')
        return true
      }

      // 本地 Hardhat 环境水龙头充值
      this.isFunding = true
      try {
        const localProvider = new JsonRpcProvider('http://127.0.0.1:8545')
        const faucetSigner = new Wallet(HARDHAT_FAUCET_PRIVATE_KEY, localProvider)
        const formattedTo = getAddress(this.address.toLowerCase())

        const tx = await faucetSigner.sendTransaction({
          to: formattedTo,
          value: parseEther('100.0')
        })
        await tx.wait()

        // 立即直连本地节点获取最新链上余额，确保 UI 秒级更新
        try {
          const latestRawBal = await localProvider.getBalance(formattedTo)
          this.balance = parseFloat(formatEther(latestRawBal)).toFixed(4)
        } catch (balErr) {
          this.balance = await this.fetchBalance(this.address)
        }

        alert(`🎉 充值划拨成功！\n已成功向您当前选中的账户 [${this.shortAddress}] 注入 100.00 测试 ETH！\n\n当前账户余额: ${this.balance} ETH\n交易哈希 (TxHash):\n${tx.hash}`)
        return true
      } catch (err) {
        console.error('测试币充值失败:', err)
        alert(`充值划拨失败: ${err.message || '请确保 Hardhat 本地 8545 节点正在运行'}`)
        return false
      } finally {
        this.isFunding = false
      }
    },

    /**
     * 智能获取最新余额
     */
    async fetchBalance(address) {
      if (!address) return '0.0000'

      try {
        const formatted = getAddress(address.toLowerCase())
        
        // 1. 优先直连 Hardhat 本地 8545 节点（实时查询最新区块余额，绕过浏览器插件缓存）
        try {
          const localProvider = new JsonRpcProvider('http://127.0.0.1:8545')
          const rawBal = await localProvider.getBalance(formatted)
          return parseFloat(formatEther(rawBal)).toFixed(4)
        } catch (localErr) {}

        // 2. 使用 window.ethereum 原生 RPC 查询
        if (typeof window.ethereum !== 'undefined') {
          try {
            const hexBalance = await window.ethereum.request({ 
              method: 'eth_getBalance', 
              params: [formatted, 'latest'] 
            })
            return parseFloat(formatEther(hexBalance)).toFixed(4)
          } catch (e) {}
        }

        // 3. 备选方案：直连 Sepolia 公共节点
        try {
          const sepoliaProvider = new JsonRpcProvider('https://rpc.sepolia.org')
          const rawBal = await sepoliaProvider.getBalance(formatted)
          return parseFloat(formatEther(rawBal)).toFixed(4)
        } catch (sepoliaErr) {}

        return '0.0000'
      } catch (err) {
        console.warn('获取账户余额异常:', err)
        return '0.0000'
      }
    },

    /**
     * 刷新当前钱包余额与网络信息。
     * 页面刷新后也会被调用，此时 isConnected 为 false，
     * 通过 eth_accounts（静默，无弹窗）检查 MetaMask 是否已有授权账户，
     * 有则自动恢复连接状态；无则保持未连接。
     */
    async refreshWalletData() {
      if (typeof window.ethereum === 'undefined') return
      try {
        const provider = new BrowserProvider(window.ethereum)

        // 使用 eth_accounts（不弹窗）获取已授权账户列表
        const accounts = await provider.send('eth_accounts', [])
        if (!accounts || accounts.length === 0) {
          // MetaMask 没有已授权账户，保持未连接状态
          this.disconnectWallet()
          return
        }

        // 自动恢复连接状态
        this.address = accounts[0]
        this.shortAddress = `${this.address.substring(0, 6)}...${this.address.substring(this.address.length - 4)}`
        this.isConnected = true

        try {
          const network = await provider.getNetwork()
          this.chainId = Number(network.chainId)
          this.chainName = network.name === 'unknown' ? `Chain ${network.chainId}` : network.name
        } catch (e) {}

        this.balance = await this.fetchBalance(this.address)

        // 监听账户/网络切换事件（刷新后重新绑定）
        if (window.ethereum.removeAllListeners) {
          window.ethereum.removeAllListeners('accountsChanged')
          window.ethereum.removeAllListeners('chainChanged')
        }
        window.ethereum.on('accountsChanged', async (newAccounts) => {
          if (!newAccounts || newAccounts.length === 0) {
            this.disconnectWallet()
          } else {
            this.address = newAccounts[0]
            this.shortAddress = `${this.address.substring(0, 6)}...${this.address.substring(this.address.length - 4)}`
            this.balance = await this.fetchBalance(this.address)
          }
        })
        window.ethereum.on('chainChanged', () => {
          this.refreshWalletData()
        })
      } catch (err) {
        console.error('自动恢复钱包连接失败:', err)
      }
    },

    /**
     * 快捷一键添加/切换至 Sepolia 测试网 (ChainID: 11155111 / 0xaa36a7)
     */
    async switchToSepolia() {
      if (typeof window.ethereum === 'undefined') return
      
      const sepoliaHexChainId = '0xaa36a7' // 11155111
      try {
        await window.ethereum.request({
          method: 'wallet_switchEthereumChain',
          params: [{ chainId: sepoliaHexChainId }]
        })
        await this.refreshWalletData()
      } catch (switchError) {
        if (switchError.code === 4902) {
          try {
            await window.ethereum.request({
              method: 'wallet_addEthereumChain',
              params: [{
                chainId: sepoliaHexChainId,
                chainName: 'Sepolia',
                rpcUrls: ['https://rpc.sepolia.org'],
                nativeCurrency: { name: 'Sepolia ETH', symbol: 'SepoliaETH', decimals: 18 },
                blockExplorerUrls: ['https://sepolia.etherscan.io']
              }]
            })
            await this.refreshWalletData()
          } catch (addError) {
            console.error('自动添加 Sepolia 测试网失败:', addError)
          }
        }
      }
    },

    /**
     * 快捷一键切换至 Hardhat 本地测试网 (127.0.0.1:8545 / ChainID: 31337)
     */
    async switchToHardhatLocalhost() {
      if (typeof window.ethereum === 'undefined') return
      
      const hardhatHexChainId = '0x7A69' // 31337
      try {
        await window.ethereum.request({
          method: 'wallet_switchEthereumChain',
          params: [{ chainId: hardhatHexChainId }]
        })
        await this.refreshWalletData()
      } catch (switchError) {
        if (switchError.code === 4902) {
          try {
            await window.ethereum.request({
              method: 'wallet_addEthereumChain',
              params: [{
                chainId: hardhatHexChainId,
                chainName: 'Hardhat Localhost (31337)',
                rpcUrls: ['http://127.0.0.1:8545'],
                nativeCurrency: { name: 'ETH', symbol: 'ETH', decimals: 18 }
              }]
            })
            await this.refreshWalletData()
          } catch (addError) {
            console.error('自动添加 Hardhat 本地节点失败:', addError)
          }
        }
      }
    },

    /**
     * 断开钱包连接
     */
    disconnectWallet() {
      this.isConnected = false
      this.address = ''
      this.shortAddress = ''
      this.balance = '0.0000'
      this.chainId = null
      this.chainName = ''
    }
  }
})
