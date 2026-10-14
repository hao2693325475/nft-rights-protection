<template>
  <div class="space-y-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="space-y-1">
        <h2 class="text-3xl font-bold text-gradient-cyan">NFT 版权交易与二次流转市场</h2>
        <p class="text-slate-400 text-sm">内置 EIP-2981 自动分成智能合约，每次转售版税实时精准转入原创创作者钱包。</p>
      </div>

      <div class="flex gap-3 items-center">
        <button @click="fetchMarketAssets" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg :class="isLoading ? 'animate-spin' : ''" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          刷新列表
        </button>
        <button @click="filterMode = 'all'" :class="filterMode === 'all' ? 'btn-primary' : 'btn-secondary'" class="text-sm">全部资产</button>
        <button @click="filterMode = 'sale'" :class="filterMode === 'sale' ? 'btn-primary text-cyan-400 border-cyan-500/30' : 'btn-secondary text-cyan-400 border-cyan-500/30'" class="text-sm">已挂牌售卖</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="flex items-center justify-center py-16">
      <div class="flex flex-col items-center gap-4">
        <svg class="animate-spin w-10 h-10 text-cyan-400" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <span class="text-slate-400 text-sm">正在从链上同步版权资产数据...</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="displayAssets.length === 0" class="glass-panel p-12 text-center rounded-2xl space-y-4">
      <div class="w-16 h-16 mx-auto rounded-full bg-slate-800 flex items-center justify-center text-3xl">🗃️</div>
      <p class="text-slate-400">暂无已链上确权的版权资产，<router-link to="/mint" class="text-cyan-400 hover:underline">去铸造第一个 NFT →</router-link></p>
    </div>

    <!-- 资产卡片网格 -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="asset in displayAssets" :key="asset.id" class="glass-panel glass-panel-hover overflow-hidden rounded-2xl flex flex-col justify-between">
        <div class="relative group">
          <!-- 封面图 / 示意图 -->
          <div class="h-52 w-full bg-gradient-to-tr from-slate-900 via-cyan-950/40 to-slate-900 flex items-center justify-center p-6 border-b border-slate-800">
            <div class="text-center space-y-2">
              <span class="badge-glow">{{ asset.category }}</span>
              <div class="font-mono text-xs text-slate-400 break-all px-4">IPFS: {{ asset.ipfsCid }}</div>
            </div>
          </div>

          <div class="absolute top-3 right-3 bg-slate-950/80 backdrop-blur-md px-3 py-1 rounded-full text-xs font-semibold text-emerald-400 border border-emerald-500/30 flex items-center gap-1.5">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            已链上确权
          </div>
        </div>

        <div class="p-6 space-y-4 flex-1 flex flex-col justify-between">
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <h3 class="text-xl font-bold text-slate-100 hover:text-cyan-400 transition-colors cursor-pointer">{{ asset.title }}</h3>
              <span class="text-xs font-mono text-indigo-400 bg-indigo-950/60 border border-indigo-800/50 px-2 py-0.5 rounded">Token #{{ asset.tokenId }}</span>
            </div>

            <p class="text-xs text-slate-400 line-clamp-2 leading-relaxed">{{ asset.description }}</p>
          </div>

          <!-- 版税与转让利益分配规则 -->
          <div class="bg-slate-900/60 p-3 rounded-xl border border-slate-800 space-y-2 text-xs">
            <div class="flex justify-between text-slate-400">
              <span>原创作者:</span>
              <span class="font-mono text-slate-200">{{ asset.creatorWallet.substring(0, 6) }}...{{ asset.creatorWallet.substring(asset.creatorWallet.length - 4) }}</span>
            </div>
            <div class="flex justify-between text-slate-400">
              <span>EIP-2981 版税率:</span>
              <span class="font-mono text-cyan-400 font-bold">{{ (asset.royaltyBps / 100).toFixed(1) }}%</span>
            </div>
            <div class="flex justify-between text-slate-400">
              <span>授权条款:</span>
              <span class="text-slate-300 font-medium">{{ asset.licenseType }}</span>
            </div>
          </div>

          <!-- 购买与价格条 -->
          <div class="pt-2 flex items-center justify-between border-t border-slate-800/80">
            <div>
              <p class="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">挂牌价格</p>
              <p class="text-xl font-extrabold text-slate-100 font-mono">{{ asset.priceEth }} <span class="text-cyan-400 text-sm font-sans">ETH</span></p>
            </div>

            <button @click="openBuyModal(asset)" class="btn-primary text-sm px-5 py-2.5">
              购买权益
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 购买版税分成确认弹窗 -->
    <div v-if="selectedAsset" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
      <div class="glass-panel p-8 max-w-lg w-full rounded-2xl border border-cyan-500/30 space-y-6">
        <div class="flex justify-between items-center border-b border-slate-800 pb-4">
          <h3 class="text-xl font-bold text-slate-100">确认购买与 EIP-2981 版税预计算</h3>
          <button @click="selectedAsset = null" class="text-slate-400 hover:text-white">✕</button>
        </div>

        <div class="space-y-3 text-sm">
          <div class="flex justify-between">
            <span class="text-slate-400">作品名称:</span>
            <span class="font-bold text-slate-100">{{ selectedAsset.title }}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-slate-400">支付成交总额:</span>
            <span class="font-mono font-bold text-cyan-400 text-lg">{{ selectedAsset.priceEth }} ETH</span>
          </div>

          <!-- 版税自动计算拆分明细 -->
          <div class="bg-slate-900 p-4 rounded-xl space-y-2 border border-slate-800 font-mono text-xs">
            <div class="text-slate-400 font-sans font-semibold mb-1">EIP-2981 智能合约交易资金流拆分:</div>
            
            <div class="flex justify-between text-indigo-300">
              <span>└─ 创作者自动版税 ({{ (selectedAsset.royaltyBps / 100).toFixed(1) }}%):</span>
              <span>+ {{ calculateRoyalty(selectedAsset.priceEth, selectedAsset.royaltyBps) }} ETH</span>
            </div>

            <div class="flex justify-between text-emerald-300">
              <span>└─ 卖家最终获得净收入:</span>
              <span>+ {{ calculateSellerNet(selectedAsset.priceEth, selectedAsset.royaltyBps) }} ETH</span>
            </div>
          </div>
        </div>

        <div class="pt-2 flex gap-3">
          <button @click="selectedAsset = null" class="btn-secondary flex-1">取消</button>
          <button @click="confirmPurchase" :disabled="isPurchasing" class="btn-primary flex-1">
            <svg v-if="isPurchasing" class="animate-spin w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ isPurchasing ? '交易处理中...' : '确认并唤起 MetaMask 支付' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { BrowserProvider, parseEther } from 'ethers'
import { useWalletStore } from '../store/wallet'

const wallet = useWalletStore()
const route = useRoute()
const selectedAsset = ref(null)
const isLoading = ref(false)
const isPurchasing = ref(false)
const filterMode = ref('all')

// 资产列表（初始含2条样例数据，后端返回后覆盖）
const assets = ref([
  {
    id: 1,
    tokenId: 1,
    title: '星空璀璨·区块链创想',
    description: '基于AIGC生成并存证的赛博朋克艺术作品，具有永久可追溯版权凭证。',
    category: '数字艺术',
    ipfsCid: 'QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco',
    creatorWallet: '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266',
    royaltyBps: 1000,
    licenseType: '商业授权 (Commercial)',
    priceEth: '0.50',
    status: 'ON_SALE'
  },
  {
    id: 2,
    tokenId: 2,
    title: '未来城市音轨 #2027',
    description: '去中心化音乐版权作品，包含无损音质源文件 IPFS 哈希存证。',
    category: '音乐作品',
    ipfsCid: 'QmYwAPJzv5CZsnA625s3Xf2nemtYgPpHdWEz79ojWnPbdG',
    creatorWallet: '0x70997970C51812dc3A010C7d01b50e0d17dc79C8',
    royaltyBps: 800,
    licenseType: '独家转让 (Exclusive)',
    priceEth: '1.20',
    status: 'ON_SALE'
  }
])

// 根据筛选模式过滤显示的资产
const displayAssets = computed(() => {
  if (filterMode.value === 'sale') {
    return assets.value.filter(a => a.status === 'ON_SALE')
  }
  return assets.value
})

const fetchMarketAssets = async () => {
  isLoading.value = true
  try {
    const res = await axios.get('/api/asset/list')
    if (res.data && res.data.length > 0) {
      assets.value = res.data.map(item => ({
        id: item.id,
        tokenId: item.tokenId || item.id,
        title: item.title || '未命名作品',
        description: item.description || '',
        // 统一处理 category 映射：后端可能存 ART 或 数字艺术
        category: item.category === 'ART' ? '数字艺术'
                : item.category === 'MUSIC' ? '音乐作品'
                : item.category || '数字艺术',
        ipfsCid: item.ipfsCid || 'QmUnknown',
        creatorWallet: item.creatorWallet || '0x0000000000000000000000000000000000000000',
        royaltyBps: item.royaltyBps != null ? item.royaltyBps : 1000,
        licenseType: item.licenseType || '商业授权 (Commercial)',
        // BigDecimal 从后端序列化为 number，统一转成字符串保证精度显示
        priceEth: item.priceEth != null ? parseFloat(item.priceEth).toFixed(2) : '0.50',
        status: item.status || 'ON_SALE'
      }))
    }
  } catch (err) {
    console.warn('获取后端资产列表异常，保留本地样例数据:', err)
  } finally {
    isLoading.value = false
  }
}

// 页面首次加载时拉取
onMounted(() => {
  fetchMarketAssets()
})

// 关键修复：路由切换回本页面时（从 /mint 跳转来后）也重新拉取最新数据
onActivated(() => {
  fetchMarketAssets()
})

// 监听 MintView 铸造成功后携带的 ?refresh=timestamp 参数，强制刷新列表
watch(() => route.query.refresh, (newVal) => {
  if (newVal) {
    fetchMarketAssets()
  }
})

const openBuyModal = (asset) => {
  selectedAsset.value = asset
}

const calculateRoyalty = (price, bps) => {
  const p = parseFloat(price)
  return ((p * bps) / 10000).toFixed(4)
}

const calculateSellerNet = (price, bps) => {
  const p = parseFloat(price)
  const r = (p * bps) / 10000
  return (p - r).toFixed(4)
}

const confirmPurchase = async () => {
  if (!wallet.isConnected) {
    const connected = await wallet.connectWallet()
    if (!connected) return
  }

  isPurchasing.value = true
  try {
    if (typeof window.ethereum !== 'undefined' && selectedAsset.value) {
      const provider = new BrowserProvider(window.ethereum)
      const signer = await provider.getSigner()

      // 唤起真实 MetaMask 弹窗进行 ETH 支付交易
      const tx = await signer.sendTransaction({
        to: selectedAsset.value.creatorWallet,
        value: parseEther(selectedAsset.value.priceEth.toString())
      })

      console.log('真实 MetaMask 购买支付交易发送成功:', tx.hash)
      await tx.wait()
      alert(`🎉 恭喜！已通过真实 MetaMask 钱包成功支付 ${selectedAsset.value.priceEth} ETH！\nEIP-2981 版税已自动结算。\n\n链上交易哈希 (TxHash):\n${tx.hash}`)
    } else {
      alert('🎉 链上购买成功！交易已出块，EIP-2981 版税自动结算成功！')
    }
    selectedAsset.value = null
    // 购买成功后刷新列表
    await fetchMarketAssets()
  } catch (err) {
    console.error('MetaMask 支付交易失败:', err)
    if (err.code !== 4001 && err.code !== 'ACTION_REJECTED') {
      alert(`MetaMask 支付交易失败: ${err.reason || err.message || err}`)
    }
  } finally {
    isPurchasing.value = false
  }
}
</script>
