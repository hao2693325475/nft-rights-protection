<template>
  <div class="space-y-12">
    <!-- Hero 视觉高光 banner -->
    <div class="glass-panel p-10 md:p-14 relative overflow-hidden rounded-3xl border border-cyan-500/20">
      <div class="absolute -top-24 -right-24 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none"></div>
      <div class="absolute -bottom-24 -left-24 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none"></div>

      <div class="max-w-3xl relative z-10 space-y-6">
        <div class="inline-flex items-center gap-2 badge-glow">
          <span class="w-2 h-2 rounded-full bg-cyan-400 animate-ping"></span>
          毕业设计成果展示工程
        </div>
        
        <h1 class="text-4xl md:text-5xl font-extrabold tracking-tight leading-tight">
          基于区块链的 <span class="text-gradient-cyan">NFT 数字资产</span><br>
          全生命周期权益保护系统
        </h1>

        <p class="text-slate-300 text-lg leading-relaxed">
          针对传统数字版权确权周期长、维权举证难以及现有 NFT 模式“铸造即侵权”漏洞与版税结算脱节等问题，
          融合 <strong class="text-cyan-400">Solidity 智能合约 (ERC-721 + EIP-2981)</strong>、<strong class="text-indigo-400">IPFS 分布式存储</strong> 与 <strong class="text-emerald-400">感知哈希 (pHash) 链下去重预审</strong>，
          打造去中心化版权确权、自动版税分成与司法电子证据链体系。
        </p>

        <div class="flex flex-wrap gap-4 pt-2">
          <router-link to="/mint" class="btn-primary text-base px-8 py-3">
            立即去确权铸造 NFT
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
          </router-link>
          
          <router-link to="/trace" class="btn-secondary text-base px-6 py-3">
            链上权属与图谱核验
          </router-link>
        </div>
      </div>
    </div>

    <!-- 论文核心四大技术创新机制展示 -->
    <div class="space-y-6">
      <div class="text-center space-y-2">
        <h2 class="text-2xl font-bold text-slate-100">核心保护机制</h2>
        <p class="text-sm text-slate-400">论文第四、五章核心功能模块对应架构</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <!-- 机制 1 -->
        <div class="glass-panel p-6 glass-panel-hover space-y-3">
          <div class="w-12 h-12 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center border border-cyan-500/20 font-bold text-xl">
            01
          </div>
          <h3 class="text-lg font-bold text-slate-100">链下查重与上链预审</h3>
          <p class="text-xs text-slate-400 leading-relaxed">
            结合 SHA-256 文件摘要碰撞比对与感知哈希 (pHash) 图像相似度检测，在铸造上链前拦截盗用与二次加工侵权作品。
          </p>
        </div>

        <!-- 机制 2 -->
        <div class="glass-panel p-6 glass-panel-hover space-y-3">
          <div class="w-12 h-12 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center border border-indigo-500/20 font-bold text-xl">
            02
          </div>
          <h3 class="text-lg font-bold text-slate-100">IPFS 双重加密存证</h3>
          <p class="text-xs text-slate-400 leading-relaxed">
            资产原文件与 ERC-721 Metadata 分布式持久化存储于 IPFS，仅上链 CID 哈希凭证，解决链上高额 Gas 与存储脱节难题。
          </p>
        </div>

        <!-- 机制 3 -->
        <div class="glass-panel p-6 glass-panel-hover space-y-3">
          <div class="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center border border-purple-500/20 font-bold text-xl">
            03
          </div>
          <h3 class="text-lg font-bold text-slate-100">EIP-2981 自动版税分成</h3>
          <p class="text-xs text-slate-400 leading-relaxed">
            基于 Solidity 开发的 RoyaltyManager 智能合约，在二级市场每次转售时强制将预设比例收益实时自动结算至创作者钱包。
          </p>
        </div>

        <!-- 机制 4 -->
        <div class="glass-panel p-6 glass-panel-hover space-y-3">
          <div class="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center border border-emerald-500/20 font-bold text-xl">
            04
          </div>
          <h3 class="text-lg font-bold text-slate-100">全网溯源与电子证据链</h3>
          <p class="text-xs text-slate-400 leading-relaxed">
            抓取合约 Event Log 构建全生命周期流转图谱，支持一键打包与导出包含时间戳、区块号与哈希密文的司法电子证据。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
