<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <img class="hero-bg" src="/home/hero-banner.png" alt="" />
      <div class="hero-copy">
        <h1>Hi, {{ greetName }}!<span class="leaf" aria-hidden="true">🍃</span></h1>
        <p>今日的努力，是为了明天更自由的你。</p>
      </div>
    </section>

    <div class="row3">
      <!-- 学习计划 -->
      <section class="island-card card plan">
        <div class="card-hd">
          <h2>学习计划</h2>
          <a class="island-link" href="#" @click.prevent>修改计划 &gt;</a>
        </div>
        <div class="plan-top">
          <div>
            <p class="plan-label">距离 CET-4 考试还有</p>
            <p class="plan-days"><strong>86</strong><span>天</span></p>
            <p class="plan-date">2025.09.20 星期六</p>
          </div>
          <div class="ring">
            <svg viewBox="0 0 96 96">
              <circle cx="48" cy="48" r="38" class="rail" />
              <circle cx="48" cy="48" r="38" class="fill" :stroke-dasharray="ringCirc" :stroke-dashoffset="ringOffset" />
            </svg>
            <div class="ring-txt"><b>62%</b><span>已完成</span></div>
          </div>
        </div>
        <div class="daily">
          <div class="daily-row"><span>今日学习进度</span><span>50 / 80 分钟</span></div>
          <div class="bar"><i style="width: 62.5%" /></div>
        </div>
        <div class="plan-ft">
          <span class="muted">今日任务已完成 3 / 6</span>
          <button type="button" class="btn" @click="navigateTo('/video')">开始学习</button>
        </div>
      </section>

      <!-- 今日任务 -->
      <section class="island-card card tasks">
        <div class="card-hd">
          <h2>今日任务</h2>
          <a class="island-link" href="#" @click.prevent>去打卡 &gt;</a>
        </div>
        <ul class="task-list">
          <li v-for="t in tasks" :key="t.id" class="task">
            <span class="task-ico" v-html="t.icon" />
            <div class="task-body">
              <div class="task-row">
                <span>{{ t.label }}</span>
                <span class="frac">{{ t.current }} / {{ t.total }}</span>
              </div>
              <div class="bar thin"><i :style="{ width: (t.current / t.total) * 100 + '%' }" /></div>
            </div>
            <span class="check" :class="{ on: t.done }" aria-hidden="true">
              <svg v-if="t.done" width="18" height="18" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" fill="var(--island-primary)"/><path d="m8 12.2 2.6 2.6L16.2 9" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="var(--island-line)" stroke-width="2"/></svg>
            </span>
          </li>
        </ul>
      </section>

      <!-- 今日单词 -->
      <section class="island-card card word">
        <div class="card-hd">
          <h2>今日单词</h2>
          <button type="button" class="island-link" @click="shuffleWord">换一组 &gt;</button>
        </div>
        <p class="term">{{ word.term }}</p>
        <p class="phon">{{ word.phonetic }}</p>
        <p class="def">{{ word.def }}</p>
        <p class="ex" v-html="word.exampleHtml" />
        <img class="word-art" src="/home/word-mascot.png" alt="" />
      </section>
    </div>

    <div class="row-mid">
      <!-- 推荐内容 -->
      <section class="island-card card rec">
        <div class="card-hd">
          <h2>推荐内容</h2>
        </div>
        <div class="tabs">
          <button v-for="tab in tabs" :key="tab" type="button" class="tab" :class="{ on: activeTab === tab }" @click="activeTab = tab">{{ tab }}</button>
        </div>
        <ul class="rec-list">
          <li v-for="item in filteredRecs" :key="item.id" class="rec-item">
            <img class="thumb" :src="item.thumb" alt="" />
            <div>
              <p class="rec-title">{{ item.title }}</p>
              <p class="rec-meta">{{ item.meta }}</p>
            </div>
          </li>
        </ul>
      </section>

      <!-- 学习数据 -->
      <section class="island-card card stats">
        <div class="card-hd">
          <h2>学习数据</h2>
          <span class="muted">本周 8.6h</span>
        </div>
        <div class="chart">
          <div v-for="d in week" :key="d.day" class="col">
            <div class="col-bar"><span :style="{ height: (d.h / maxH) * 100 + '%' }" /></div>
            <em>{{ d.day }}</em>
          </div>
        </div>
        <div class="stat-grid">
          <div><b>5</b><span>学习天数</span></div>
          <div><b>235</b><span>累计词汇</span></div>
          <div><b>3</b><span>完成套题</span></div>
        </div>
      </section>

      <!-- 岛屿成就 -->
      <section class="island-card card ach">
        <div class="card-hd">
          <h2>岛屿成就</h2>
          <a class="island-link" href="#" @click.prevent>查看全部 &gt;</a>
        </div>
        <div class="ach-main">
          <img class="badge" src="/home/badge-lv.png" width="88" height="88" alt="Lv.12" />
          <div class="ach-exp">
            <div class="exp-row"><span class="exp-tag">EXP</span><span class="muted">320 / 500</span></div>
            <div class="bar"><i style="width: 64%" /></div>
          </div>
        </div>
        <div class="badges">
          <div v-for="b in badges" :key="b" class="mini" :class="{ lock: b.lock }">
            <span>{{ b.icon }}</span>
            <em>{{ b.label }}</em>
          </div>
        </div>
      </section>
    </div>

    <div class="row-bot">
      <section class="island-card card tools">
        <div class="card-hd"><h2>快捷工具</h2></div>
        <div class="tool-row">
          <button v-for="t in tools" :key="t.label" type="button" class="tool" @click="onTool(t)">
            <span class="tool-ico" v-html="t.icon" />
            <em>{{ t.label }}</em>
          </button>
        </div>
      </section>

      <section class="island-card card radio">
        <img class="radio-bg" src="/home/radio-bg.png" alt="" />
        <div class="radio-body">
          <div>
            <p class="radio-title">岛屿电台</p>
            <p class="muted">治愈语录电台 · 陪你一起学习</p>
          </div>
          <button type="button" class="play" aria-label="播放" @click="radioOn = !radioOn">
            <svg v-if="!radioOn" width="18" height="18" viewBox="0 0 24 24" fill="#fff"><path d="M8 6.5v11l10-5.5L8 6.5Z"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="#fff"><rect x="7" y="6" width="3.5" height="12" rx="1"/><rect x="13.5" y="6" width="3.5" height="12" rx="1"/></svg>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const message = useAppMessage()
const greetName = computed(() => auth.user?.nickname || 'Islander')

const ringCirc = 2 * Math.PI * 38
const ringOffset = ringCirc * (1 - 0.62)

const ico = {
  word: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--island-primary)" stroke-width="1.7"><path d="M5 4.5A2.5 2.5 0 0 1 7.5 2H20v16H7.5A2.5 2.5 0 0 0 5 20.5V4.5Z"/></svg>`,
  listen: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--island-primary)" stroke-width="1.7"><path d="M4 12v-1a8 8 0 0 1 16 0v1"/><path d="M6 12v3a3 3 0 0 0 3 3"/><path d="M18 12v2a2 2 0 0 1-2 2"/></svg>`,
  read: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--island-primary)" stroke-width="1.7"><path d="M3 6.5C5.5 5 8.5 5 12 6.5S18.5 8 21 6.5V18c-2.5 1.5-5.5 1.5-9 0s-6.5-1.5-9 0V6.5Z"/></svg>`,
  write: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--island-primary)" stroke-width="1.7"><path d="M13.5 5.5 18.5 10.5"/><path d="M4 20l4.2-1.1L19 8.1a2.1 2.1 0 0 0 0-3L17 3a2.1 2.1 0 0 0-3 0L5.1 11.8 4 16v4Z"/></svg>`
}

const tasks = [
  { id: 1, label: '背单词 50个', current: 30, total: 50, done: false, icon: ico.word },
  { id: 2, label: '听力练习', current: 20, total: 20, done: true, icon: ico.listen },
  { id: 3, label: '阅读理解', current: 1, total: 2, done: false, icon: ico.read },
  { id: 4, label: '写作练习', current: 0, total: 1, done: false, icon: ico.write }
]

const wordBank = [
  {
    term: 'serendipity',
    phonetic: '/ˌserənˈdɪpəti/',
    def: 'n. 意外发现珍奇事物的本领',
    exampleHtml: 'It was <b>serendipity</b> that led me to this amazing book.'
  },
  {
    term: 'resilient',
    phonetic: '/rɪˈzɪliənt/',
    def: 'adj. 有韧性的；能迅速恢复的',
    exampleHtml: 'She remained <b>resilient</b> after setbacks.'
  },
  {
    term: 'eloquent',
    phonetic: '/ˈeləkwənt/',
    def: 'adj. 雄辩的；有说服力的',
    exampleHtml: 'His <b>eloquent</b> speech moved the audience.'
  }
]
const wordIndex = ref(0)
const word = computed(() => wordBank[wordIndex.value]!)
function shuffleWord() {
  wordIndex.value = (wordIndex.value + 1) % wordBank.length
}

const tabs = ['精选', '听力', '阅读', '写作', '真题']
const activeTab = ref('精选')
const recommendations = [
  { id: 1, tab: '精选', title: '四六级高频词汇记忆技巧', meta: '12 分钟 · 2.3k 人学习', thumb: '/home/rec-thumb-1.png' },
  { id: 2, tab: '听力', title: '长对话信息抓取训练', meta: '18 分钟 · 1.1k 人学习', thumb: '/home/rec-thumb-2.png' },
  { id: 3, tab: '阅读', title: '段落主旨快速定位法', meta: '15 分钟 · 980 人学习', thumb: '/home/rec-thumb-3.png' },
  { id: 4, tab: '精选', title: '双语精听入门指南', meta: '8 分钟 · 3.1k 人学习', thumb: '/home/rec-thumb-1.png' }
]
const filteredRecs = computed(() =>
  recommendations.filter((r) => activeTab.value === '精选' || r.tab === activeTab.value).slice(0, 3)
)

const week = [
  { day: '一', h: 1.2 }, { day: '二', h: 2.0 }, { day: '三', h: 0.8 },
  { day: '四', h: 2.5 }, { day: '五', h: 1.6 }, { day: '六', h: 3.0 }, { day: '日', h: 1.0 }
]
const maxH = Math.max(...week.map((d) => d.h))

const badges = [
  { label: '连续7天', icon: '7', lock: false },
  { label: '词汇500', icon: '词', lock: false },
  { label: '听力达人', icon: '听', lock: false },
  { label: '满勤月', icon: '月', lock: true }
]

const tools = [
  { label: '生词本', to: '/vocabulary?tab=notebook', icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M5 4.5A2.5 2.5 0 0 1 7.5 2H20v16H7.5A2.5 2.5 0 0 0 5 20.5V4.5Z"/><path d="M9 7h6M9 11h4"/></svg>` },
  { label: '错题本', soon: true, icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M7 3h10a2 2 0 0 1 2 2v14l-7-2.5L5 19V5a2 2 0 0 1 2-2Z"/><path d="m10 9 4 4M14 9l-4 4"/></svg>` },
  { label: '收藏夹', soon: true, icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M8 3h8v18l-4-2.5L8 21V3Z"/></svg>` },
  { label: '笔记本', soon: true, icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M7 3h11a1 1 0 0 1 1 1v16l-4-2-4 2V4a1 1 0 0 0-1-1H7"/><path d="M7 3a2 2 0 0 0-2 2v14"/></svg>` },
  { label: '学习日历', soon: true, icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4M16 3v4M4 10h16"/></svg>` },
  { label: '学习设置', to: '/me', icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="12" r="3"/><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4"/></svg>` }
]

function onTool(t: { label: string; to?: string; soon?: boolean }) {
  if (t.to) return navigateTo(t.to)
  message.info(`${t.label}即将开放`)
}

const radioOn = ref(false)
onMounted(() => auth.hydrate())
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 1120px;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  min-height: 200px;
  isolation: isolate;
}

.hero-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center right;
  z-index: 0;
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 22rem;
  padding: 1.6rem 1.5rem 2rem;
}

.hero-copy h1 {
  font-family: var(--font-display);
  font-size: clamp(1.7rem, 3vw, 2.15rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--island-text);
  line-height: 1.15;
}

.leaf {
  margin-left: 0.2rem;
  font-size: 0.85em;
}

.hero-copy p {
  margin-top: 0.55rem;
  color: var(--island-text);
  font-size: 0.92rem;
  line-height: 1.55;
  opacity: 0.85;
}

.card {
  padding: 1.1rem 1.15rem 1.2rem;
}

.card-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.85rem;
}

.card-hd h2 {
  font-size: 1rem;
  font-weight: 700;
  color: var(--island-text);
}

.muted {
  color: var(--island-muted);
  font-size: 0.78rem;
}

.row3,
.row-mid {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

@media (min-width: 900px) {
  .row3,
  .row-mid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.plan-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.plan-label {
  font-size: 0.8rem;
  color: var(--island-muted);
}

.plan-days {
  margin-top: 0.2rem;
  color: var(--island-text);
}

.plan-days strong {
  font-family: var(--font-display);
  font-size: 2.4rem;
  font-weight: 700;
  line-height: 1;
  color: var(--island-forest);
}

.plan-days span {
  margin-left: 0.2rem;
  font-size: 1rem;
  font-weight: 600;
}

.plan-date {
  margin-top: 0.35rem;
  font-size: 0.75rem;
  color: var(--island-muted);
}

.ring {
  position: relative;
  width: 88px;
  height: 88px;
  flex-shrink: 0;
}

.ring svg {
  width: 88px;
  height: 88px;
  transform: rotate(-90deg);
}

.rail {
  fill: none;
  stroke: var(--island-sage-soft);
  stroke-width: 8;
}

.fill {
  fill: none;
  stroke: var(--island-primary);
  stroke-width: 8;
  stroke-linecap: round;
}

.ring-txt {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-txt b {
  font-family: var(--font-display);
  font-size: 1rem;
}

.ring-txt span {
  font-size: 0.62rem;
  color: var(--island-muted);
}

.daily {
  margin-top: 1rem;
}

.daily-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.35rem;
  font-size: 0.78rem;
  color: var(--island-muted);
}

.bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--island-sage-soft);
}

.bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6f8f72, var(--island-primary));
}

.bar.thin { height: 6px; }

.plan-ft {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-top: 1rem;
}

.btn {
  border: none;
  border-radius: 999px;
  background: var(--island-forest);
  color: #fff;
  font-size: 0.88rem;
  font-weight: 600;
  padding: 0.55rem 1.15rem;
  cursor: pointer;
  white-space: nowrap;
}

.btn:hover { background: var(--island-forest-deep); }

.task-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.task {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.task-ico {
  display: inline-flex;
  width: 1.8rem;
  height: 1.8rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--island-sage-soft);
}

.task-body { flex: 1; min-width: 0; }

.task-row {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.3rem;
  font-size: 0.86rem;
}

.frac {
  color: var(--island-muted);
  font-size: 0.75rem;
  flex-shrink: 0;
}

.word {
  position: relative;
  overflow: hidden;
  min-height: 240px;
}

.term {
  font-family: var(--font-display);
  font-size: 1.65rem;
  font-weight: 700;
  color: var(--island-forest);
  letter-spacing: -0.02em;
}

.phon {
  margin-top: 0.25rem;
  color: var(--island-muted);
  font-size: 0.82rem;
}

.def {
  margin-top: 0.7rem;
  font-size: 0.9rem;
  line-height: 1.55;
}

.ex {
  margin-top: 0.55rem;
  max-width: 70%;
  color: var(--island-muted);
  font-size: 0.8rem;
  line-height: 1.5;
}

.ex :deep(b) {
  color: var(--island-primary);
  font-weight: 700;
}

.word-art {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 46%;
  max-width: 180px;
  pointer-events: none;
  object-fit: contain;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-bottom: 0.85rem;
}

.tab {
  border: none;
  border-radius: 999px;
  background: var(--island-surface);
  color: var(--island-muted);
  font-size: 0.75rem;
  padding: 0.28rem 0.7rem;
  cursor: pointer;
}

.tab.on {
  background: var(--island-sage);
  color: var(--island-forest);
  font-weight: 600;
}

.rec-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.rec-item {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.thumb {
  width: 4.2rem;
  height: 2.8rem;
  flex-shrink: 0;
  border-radius: 12px;
  object-fit: cover;
}

.rec-title {
  font-size: 0.88rem;
  font-weight: 600;
}

.rec-meta {
  margin-top: 0.2rem;
  font-size: 0.72rem;
  color: var(--island-muted);
}

.chart {
  display: flex;
  align-items: flex-end;
  gap: 0.45rem;
  height: 108px;
  margin-bottom: 0.9rem;
}

.col {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.col-bar {
  display: flex;
  flex: 1;
  width: 100%;
  align-items: flex-end;
  justify-content: center;
}

.col-bar span {
  width: 70%;
  max-width: 18px;
  min-height: 6px;
  border-radius: 8px 8px 4px 4px;
  background: linear-gradient(180deg, #8aaa86, var(--island-primary));
}

.col em {
  margin-top: 0.3rem;
  font-size: 0.68rem;
  font-style: normal;
  color: var(--island-muted);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.4rem;
  border-top: 1px solid var(--island-line);
  padding-top: 0.8rem;
}

.stat-grid b {
  display: block;
  font-family: var(--font-display);
  font-size: 1.15rem;
}

.stat-grid span {
  font-size: 0.7rem;
  color: var(--island-muted);
}

.ach-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.badge {
  object-fit: contain;
}

.ach-exp { width: 100%; }

.exp-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}

.exp-tag {
  border-radius: 999px;
  background: var(--island-sage-soft);
  color: var(--island-primary);
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.12rem 0.45rem;
}

.badges {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.4rem;
  margin-top: 1rem;
}

.mini {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.65rem;
  color: var(--island-muted);
  text-align: center;
}

.mini span {
  display: flex;
  width: 2.2rem;
  height: 2.2rem;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--island-sage-soft);
  color: var(--island-primary);
  font-weight: 700;
}

.mini.lock { opacity: 0.4; }
.mini.lock span { background: #e6ebe4; color: #9aa69c; }

.row-bot {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

@media (min-width: 900px) {
  .row-bot {
    grid-template-columns: 1.55fr 1fr;
  }
}

.tool-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.55rem;
}

@media (min-width: 640px) {
  .tool-row { grid-template-columns: repeat(6, 1fr); }
}

.tool {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  border: none;
  background: transparent;
  color: var(--island-text);
  cursor: pointer;
  padding: 0.25rem;
}

.tool-ico {
  display: flex;
  width: 2.7rem;
  height: 2.7rem;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
  color: var(--island-forest);
  box-shadow: 0 1px 4px rgba(45, 71, 57, 0.06);
}

.tool em {
  font-size: 0.72rem;
  font-style: normal;
  color: var(--island-muted);
}

.tool:hover .tool-ico {
  background: var(--island-sage-soft);
}

.radio {
  position: relative;
  overflow: hidden;
  min-height: 118px;
  padding: 0;
}

.radio-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.55;
}

.radio-body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  height: 100%;
  min-height: 118px;
  padding: 1.1rem 1.2rem;
  background: linear-gradient(90deg, rgba(248, 249, 244, 0.92) 0%, rgba(248, 249, 244, 0.55) 55%, rgba(248, 249, 244, 0.2) 100%);
}

.radio-title {
  font-size: 1rem;
  font-weight: 700;
}

.play {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.7rem;
  height: 2.7rem;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  background: var(--island-forest);
  cursor: pointer;
}
</style>
