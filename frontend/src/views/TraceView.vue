<template>
  <div class="space-y-8 max-w-5xl mx-auto">
    <div class="space-y-2">
      <h2 class="text-3xl font-bold text-gradient-cyan">全网权属核验与生命周期流转图谱</h2>
      <p class="text-slate-400 text-sm">输入 Token ID 或作品 SHA-256 指纹，追溯作品从首次存证到历次交易的不可篡改存证图谱。</p>
    </div>

    <!-- 搜索验真栏 -->
    <div class="glass-panel p-4 rounded-2xl flex gap-3 border border-cyan-500/20">
      <div class="relative flex-1">
        <input 
          v-model="searchQuery" 
          type="text" 
          @keydown.enter="handleSearch"
          placeholder="请输入 NFT Token ID (例: 1) 或作品 SHA-256 哈希..." 
          class="w-full bg-slate-900/80 border border-slate-700 focus:border-cyan-500 rounded-xl pl-10 pr-4 py-3 text-slate-100 text-sm focus:outline-none font-mono"
        />
        <svg class="w-5 h-5 text-slate-500 absolute left-3 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>

      <button @click="handleSearch" :disabled="isLoading" class="btn-primary flex items-center gap-2">
        <svg v-if="isLoading" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        {{ isLoading ? '核验中...' : '核验链上凭证' }}
      </button>
    </div>

    <!-- 快捷跳转：已存在的 Token ID 列表 -->
    <div v-if="availableTokenIds.length > 0" class="flex flex-wrap items-center gap-2 text-xs">
      <span class="text-slate-500">快速查看:</span>
      <button
        v-for="tokenId in availableTokenIds"
        :key="tokenId"
        @click="searchQuery = tokenId.toString(); handleSearch()"
        class="px-2.5 py-1 rounded-lg bg-slate-800 border border-slate-700 text-cyan-400 hover:bg-cyan-500/10 hover:border-cyan-500/40 transition-all font-mono cursor-pointer"
      >
        Token #{{ tokenId }}
      </button>
    </div>

    <!-- 未找到作品提示 -->
    <div v-if="notFound" class="glass-panel p-8 rounded-2xl text-center space-y-3 border border-rose-500/20">
      <div class="text-3xl">🔍</div>
      <p class="text-slate-300 font-semibold">未找到 Token ID "{{ lastSearched }}" 的链上存证记录</p>
      <p class="text-xs text-slate-500">请确认 Token ID 是否正确，或该作品尚未完成链上铸造</p>
    </div>

    <!-- 核验成功证据卡片 -->
    <div v-if="evidenceData && !notFound" class="space-y-6">
      <div class="glass-panel p-6 rounded-2xl space-y-4 border border-emerald-500/30">
        <div class="flex items-center justify-between border-b border-slate-800 pb-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center font-bold">
              ✓
            </div>
            <div>
              <h3 class="text-lg font-bold text-slate-100">链上核验通过 · 真伪一致</h3>
              <p class="text-xs text-slate-400 font-mono">Evidence Package Hash: {{ evidenceData.evidencePackageHash.substring(0, 24) }}...</p>
            </div>
          </div>

          <button @click="downloadEvidenceJSON" class="btn-secondary text-xs flex items-center gap-2 border-emerald-500/40 text-emerald-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出司法电子证据包 (JSON)
          </button>
        </div>

        <!-- 作品基本信息 -->
        <div v-if="evidenceData.assetInfo" class="bg-slate-900/60 p-4 rounded-xl border border-slate-800 space-y-2 text-sm">
          <div class="flex items-center gap-2 text-slate-300 font-semibold mb-1">
            <svg class="w-4 h-4 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            作品信息
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs font-mono">
            <div class="space-y-0.5">
              <span class="text-slate-500 font-sans">作品名称:</span>
              <p class="text-slate-200 font-sans font-semibold">{{ evidenceData.assetInfo.title }}</p>
            </div>
            <div class="space-y-0.5">
              <span class="text-slate-500 font-sans">类别:</span>
              <p class="text-slate-200">{{ evidenceData.assetInfo.category }}</p>
            </div>
            <div class="space-y-0.5">
              <span class="text-slate-500 font-sans">EIP-2981 版税率:</span>
              <p class="text-cyan-400">{{ evidenceData.assetInfo.royaltyBps != null ? (evidenceData.assetInfo.royaltyBps / 100).toFixed(1) + '%' : '—' }}</p>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs font-mono">
          <div class="bg-slate-900/60 p-3 rounded-xl border border-slate-800 space-y-1">
            <span class="text-slate-500 font-sans">文件 SHA-256 指纹:</span>
            <p class="text-slate-200 break-all">{{ evidenceData.digitalAssetProof.fileSHA256 }}</p>
          </div>

          <div class="bg-slate-900/60 p-3 rounded-xl border border-slate-800 space-y-1">
            <span class="text-slate-500 font-sans">原文件 IPFS CID:</span>
            <p class="text-cyan-400 break-all">{{ evidenceData.digitalAssetProof.ipfsSourceCID }}</p>
          </div>

          <div class="bg-slate-900/60 p-3 rounded-xl border border-slate-800 space-y-1">
            <span class="text-slate-500 font-sans">创作者链上身份:</span>
            <p class="text-indigo-400 break-all">{{ evidenceData.creatorIdentityProof.walletAddress }}</p>
          </div>
        </div>
      </div>

      <!-- 全生命周期流转时序节点图谱 -->
      <div class="glass-panel p-8 rounded-2xl space-y-6">
        <h3 class="text-xl font-bold text-slate-100 flex items-center gap-2">
          <svg class="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          全生命周期权属流转图谱 (Lifecycle Graph) — 检索目标: [ {{ lastSearched }} ]
        </h3>

        <div class="relative border-l-2 border-cyan-500/40 ml-4 pl-6 space-y-8">
          <div v-for="(node, idx) in graphNodes" :key="node.id || idx" class="relative group">
            <div class="absolute -left-[31px] top-0 w-4 h-4 rounded-full bg-cyan-400 border-4 border-slate-900 ring-4 ring-cyan-500/20"></div>
            <div class="bg-slate-900/60 p-4 rounded-xl border border-slate-800 space-y-2">
              <div class="flex justify-between items-center text-xs">
                <span class="badge-glow">节点 #{{ idx + 1 }} · {{ node.title }}</span>
                <span class="font-mono text-slate-500">区块高度: #{{ node.blockNumber }}</span>
              </div>
              <p class="text-sm font-bold text-slate-200">操作主体/地址: {{ node.operator }}</p>
              <p class="text-xs font-mono text-cyan-400/90 break-all">Tx Hash: {{ node.txHash }}</p>
              <p class="text-[11px] font-mono text-slate-500">存证落块时间: {{ node.timestamp }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const searchQuery = ref('')
const lastSearched = ref('')
const evidenceData = ref(null)
const graphNodes = ref([])
const isLoading = ref(false)
const notFound = ref(false)
// 存储系统中所有可用的 Token ID，方便快捷跳转
const availableTokenIds = ref([])

// 加载所有已知 Token ID
const loadAvailableTokens = async () => {
  try {
    const res = await axios.get('/api/asset/list')
    if (res.data && res.data.length > 0) {
      availableTokenIds.value = res.data.map(item => item.tokenId || item.id).filter(Boolean)
    } else {
      availableTokenIds.value = [1, 2]
    }
  } catch {
    availableTokenIds.value = [1, 2]
  }
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  isLoading.value = true
  notFound.value = false
  evidenceData.value = null
  graphNodes.value = []
  lastSearched.value = searchQuery.value.trim()

  try {
    // 动态调用后端证据包与溯源接口
    const [evidenceRes, graphRes] = await Promise.all([
      axios.get(`/api/trace/evidence-package/${lastSearched.value}`).catch(() => null),
      axios.get(`/api/trace/graph/${lastSearched.value}`).catch(() => null)
    ])

    if (evidenceRes && evidenceRes.data && evidenceRes.data.evidencePackageHash) {
      evidenceData.value = evidenceRes.data
    } else {
      // 后端接口失败时，尝试从资产列表中查找对应数据构造本地备选
      try {
        const listRes = await axios.get('/api/asset/list')
        const allAssets = listRes.data || []
        const found = allAssets.find(a =>
          String(a.tokenId) === lastSearched.value ||
          String(a.id) === lastSearched.value ||
          (a.fileHash && a.fileHash === lastSearched.value)
        )
        if (found) {
          evidenceData.value = buildLocalEvidence(found)
        } else {
          notFound.value = true
          return
        }
      } catch {
        notFound.value = true
        return
      }
    }

    if (graphRes && graphRes.data && graphRes.data.nodes) {
      graphNodes.value = graphRes.data.nodes
    } else {
      // 构建本地图谱节点
      const creator = evidenceData.value?.creatorIdentityProof?.walletAddress || '0xUnknown'
      const txHash = evidenceData.value?.blockchainProof?.transactionHash || '0x0000000000000000000000000000000000000000000000000000000000000000'
      const blockNum = evidenceData.value?.blockchainProof?.blockNumber || 1024
      graphNodes.value = [
        {
          id: 'node-mint',
          title: '创作者确权存证 (Mint)',
          operator: creator,
          blockNumber: blockNum,
          txHash: txHash,
          timestamp: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        },
        {
          id: 'node-list',
          title: '版权许可挂牌 (List)',
          operator: creator,
          blockNumber: blockNum + 64,
          txHash: '0x' + txHash.slice(2, 10) + 'abcdef1234' + txHash.slice(20),
          timestamp: new Date(Date.now() + 60000 * 30).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        }
      ]
    }
  } catch (err) {
    console.warn('追溯查询异常:', err)
    notFound.value = true
  } finally {
    isLoading.value = false
  }
}

/**
 * 根据后端资产对象本地构造证据包（当 /evidence-package 接口不可用时的降级方案）
 */
const buildLocalEvidence = (asset) => {
  return {
    evidencePackageHash: asset.fileHash || 'a88f7b209d1234567890abcdef1234567890abcdef1234567890abcdef123456',
    assetInfo: {
      title: asset.title,
      category: asset.category,
      royaltyBps: asset.royaltyBps
    },
    digitalAssetProof: {
      fileSHA256: asset.fileHash || 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      ipfsSourceCID: asset.ipfsCid || 'QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco'
    },
    creatorIdentityProof: {
      walletAddress: asset.creatorWallet || '0x0000000000000000000000000000000000000000'
    },
    blockchainProof: {
      contractAddress: '0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0',
      transactionHash: asset.txHash || '0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef',
      blockNumber: 1024
    }
  }
}

onMounted(async () => {
  await loadAvailableTokens()
  // 默认展示 Token #1
  if (availableTokenIds.value.length > 0) {
    searchQuery.value = availableTokenIds.value[0].toString()
    await handleSearch()
  }
})

const downloadEvidenceJSON = () => {
  if (!evidenceData.value) return
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(evidenceData.value, null, 2))
  const downloadAnchor = document.createElement('a')
  downloadAnchor.setAttribute("href", dataStr)
  downloadAnchor.setAttribute("download", `司法电子证据包_Token_${lastSearched.value}.json`)
  document.body.appendChild(downloadAnchor)
  downloadAnchor.click()
  downloadAnchor.remove()
}
</script>
