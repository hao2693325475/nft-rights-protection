<template>
  <div class="min-h-screen flex flex-col">
    <!-- 顶部全宽玻璃顶栏 -->
    <header class="sticky top-0 z-50 glass-panel border-b border-slate-800/80 px-6 py-4">
      <div class="max-w-7xl mx-auto flex items-center justify-between">
        <!-- 系统 Logo 与标题 -->
        <router-link to="/" class="flex items-center gap-3 group">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-cyan-500/20 group-hover:scale-105 transition-transform">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <div>
            <h1 class="text-xl font-bold tracking-tight text-gradient-cyan">NFT Asset Rights</h1>
            <p class="text-xs text-slate-400">基于区块链的NFT数字资产权益保护系统</p>
          </div>
        </router-link>

        <!-- 导航菜单 -->
        <nav class="hidden md:flex items-center gap-1 bg-slate-900/60 p-1.5 rounded-xl border border-slate-800">
          <router-link to="/" class="nav-item" active-class="nav-item-active">系统首页</router-link>
          <router-link to="/mint" class="nav-item" active-class="nav-item-active">确权存证</router-link>
          <router-link to="/market" class="nav-item" active-class="nav-item-active">版权市场</router-link>
          <router-link to="/trace" class="nav-item" active-class="nav-item-active">全网溯源</router-link>
          <router-link to="/admin" class="nav-item" active-class="nav-item-active">后台管理</router-link>
        </nav>

        <!-- Web3 真实 MetaMask 钱包连接组件 -->
        <div class="flex items-center gap-3">

          <!-- 可点击切换的下拉网络选择器组件 -->
          <div v-if="wallet.isConnected" class="relative">
            <button 
              @click="showNetworkDropdown = !showNetworkDropdown"
              class="px-3 py-1.5 rounded-lg bg-slate-900/90 border border-slate-700/80 hover:border-cyan-500/50 text-xs font-mono text-slate-200 flex items-center gap-2 transition-all cursor-pointer shadow-sm"
              title="点击下拉选择区块链网络"
            >
              <span class="w-2 h-2 rounded-full" :class="wallet.chainId === 31337 ? 'bg-cyan-400 animate-pulse' : wallet.chainId === 11155111 ? 'bg-amber-400 animate-pulse' : 'bg-slate-400'"></span>
              <span>{{ wallet.chainId === 31337 ? 'Hardhat 本地网' : wallet.chainId === 11155111 ? 'Sepolia 测试网' : `Chain: ${wallet.chainId}` }}</span>
              <svg class="w-3.5 h-3.5 text-slate-400 transition-transform duration-200" :class="showNetworkDropdown ? 'rotate-180 text-cyan-400' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <!-- 浮动下拉菜单列表 -->
            <div 
              v-if="showNetworkDropdown" 
              class="absolute right-0 mt-2 w-60 glass-panel rounded-xl shadow-2xl border border-slate-700/90 py-1.5 z-50 animate-in fade-in duration-150"
            >
              <div class="px-3 py-1.5 text-[10px] font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-800/80 mb-1">
                选择区块链 RPC 网络
              </div>

              <!-- 选项 1: Hardhat 本地网 -->
              <button 
                @click="selectNetwork('hardhat')" 
                class="w-full text-left px-3 py-2 text-xs flex items-center justify-between hover:bg-cyan-500/10 transition-colors cursor-pointer"
                :class="wallet.chainId === 31337 ? 'text-cyan-400 font-bold bg-cyan-500/5' : 'text-slate-300'"
              >
                <div class="flex items-center gap-2">
                  <span class="text-base">⚡</span>
                  <div>
                    <p class="font-semibold">Hardhat 本地网</p>
                    <p class="text-[10px] text-slate-500 font-mono">127.0.0.1:8545 (31337)</p>
                  </div>
                </div>
                <span v-if="wallet.chainId === 31337" class="text-cyan-400 font-bold">✓</span>
              </button>

              <!-- 选项 2: Sepolia 公测网 -->
              <button 
                @click="selectNetwork('sepolia')" 
                class="w-full text-left px-3 py-2 text-xs flex items-center justify-between hover:bg-amber-500/10 transition-colors cursor-pointer"
                :class="wallet.chainId === 11155111 ? 'text-amber-400 font-bold bg-amber-500/5' : 'text-slate-300'"
              >
                <div class="flex items-center gap-2">
                  <span class="text-base">🌐</span>
                  <div>
                    <p class="font-semibold">Sepolia 测试网</p>
                    <p class="text-[10px] text-slate-500 font-mono">以太坊公测网 (11155111)</p>
                  </div>
                </div>
                <span v-if="wallet.chainId === 11155111" class="text-amber-400 font-bold">✓</span>
              </button>
            </div>
          </div>

          <!-- 快捷一键划拨充值 100 ETH -->
          <button 
            v-if="wallet.isConnected" 
            @click="wallet.requestFaucetETH()" 
            :disabled="wallet.isFunding"
            class="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs hover:bg-emerald-500/20 transition-all cursor-pointer font-semibold"
            title="点击从 Hardhat 本地节点水龙头向您当前选中的钱包充值 100 ETH"
          >
            <span>{{ wallet.isFunding ? '充值划拨中...' : '💧 领 100 ETH' }}</span>
          </button>

          <!-- 余额展示 -->
          <div v-if="wallet.isConnected" class="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700/60 text-xs">
            <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span class="text-slate-300 font-mono">{{ wallet.balance }} ETH</span>
          </div>

          <!-- 未连接按钮 -->
          <button v-if="!wallet.isConnected" @click="handleConnect" class="btn-primary">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            连接 MetaMask 钱包
          </button>

          <!-- 已连接按钮 (点击弹出 MetaMask 账号选择切换) -->
          <button 
            v-else 
            @click="wallet.requestChangeAccount()" 
            class="btn-secondary font-mono text-xs text-cyan-400 border-cyan-500/30 hover:bg-cyan-500/10 hover:border-cyan-400 transition-all flex items-center gap-1.5 cursor-pointer"
            title="点击唤起 MetaMask 账号选择框切换连接账号"
          >
            <span>{{ wallet.shortAddress }}</span>
            <svg class="w-3.5 h-3.5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <!-- 视图呈现区域 -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-6">
      <router-view />
    </main>

    <!-- 底部页脚 -->
    <footer class="border-t border-slate-800/80 py-6 text-center text-xs text-slate-500 glass-panel mt-12">
      <div class="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between px-6 gap-4">
        <div>© 2027 基于区块链的NFT数字资产权益保护系统 | 毕业设计工程 implementation</div>
        <div class="flex gap-4">
          <span>Solidity v0.8.19</span>
          <span>•</span>
          <span>ERC-721 + EIP-2981</span>
          <span>•</span>
          <span>IPFS (Kubo)</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useWalletStore } from './store/wallet'

const wallet = useWalletStore()
const showNetworkDropdown = ref(false)

const selectNetwork = (type) => {
  showNetworkDropdown.value = false
  if (type === 'hardhat') {
    wallet.switchToHardhatLocalhost()
  } else if (type === 'sepolia') {
    wallet.switchToSepolia()
  }
}

const handleConnect = async () => {
  await wallet.connectWallet()
}

onMounted(() => {
  // 页面加载时自动检查已有连接状态
  if (typeof window.ethereum !== 'undefined') {
    wallet.refreshWalletData()
  }
})
</script>

<style scoped>
.nav-item {
  padding: 6px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #94A3B8;
  border-radius: 8px;
  transition: all 0.2s ease;
  text-decoration: none;
}

.nav-item:hover {
  color: #F1F5F9;
  background: rgba(255, 255, 255, 0.05);
}

.nav-item-active {
  color: #38BDF8 !important;
  background: rgba(56, 189, 248, 0.12) !important;
  font-weight: 600;
}
</style>
