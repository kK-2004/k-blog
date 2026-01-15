<script setup lang="ts">
import { ref, onMounted } from 'vue'

const goHome = () => {
  console.log('goHome')
  window.location.hash = '#/blog'
}

// 浮动元素的随机位置
const floatingElements = ref([
  { x: 10, y: 20, delay: 0, duration: 8 },
  { x: 80, y: 15, delay: 1, duration: 10 },
  { x: 15, y: 70, delay: 2, duration: 12 },
  { x: 85, y: 65, delay: 1.5, duration: 9 },
  { x: 50, y: 85, delay: 0.5, duration: 11 }
])

// 鼠标移动效果
const mouseX = ref(0)
const mouseY = ref(0)

const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = (e.clientX / window.innerWidth - 0.5) * 20
  mouseY.value = (e.clientY / window.innerHeight - 0.5) * 20
}

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <div class="not-found-wrapper">
    <!-- 背景装饰层 -->
    <div class="background-layer">
      <!-- 渐变网格 -->
      <div class="gradient-mesh"></div>

      <!-- 浮动装饰元素 -->
      <div
          v-for="(elem, i) in floatingElements"
          :key="i"
          class="floating-orb"
          :style="{
          left: `${elem.x}%`,
          top: `${elem.y}%`,
          animationDelay: `${elem.delay}s`,
          animationDuration: `${elem.duration}s`
        }"
      ></div>

      <!-- 噪点纹理 -->
      <div class="noise-texture"></div>
    </div>

    <!-- 主内容卡片 -->
    <div
        class="content-card"
        :style="{
        transform: `perspective(1000px) rotateX(${mouseY * -0.3}deg) rotateY(${mouseX * 0.3}deg)`
      }"
    >
      <!-- 光晕效果 -->
      <div class="card-glow"></div>

      <!-- 404 数字 - 分离显示 -->
      <div class="number-container">
        <div class="number-digit" style="animation-delay: 0s">4</div>
        <div class="number-zero" style="animation-delay: 0.1s">0</div>
        <div class="number-digit" style="animation-delay: 0.2s">4</div>
      </div>

      <!-- 装饰线条 -->
      <div class="decorative-line"></div>

      <!-- 文字内容 -->
      <div class="text-content">
        <h1 class="main-message">页面走丢了</h1>
        <p class="sub-message">
          页面找不到，不如先回家看看？
        </p>
      </div>

      <!-- 返回按钮 -->
      <button @click="goHome" class="home-button">
        <i class="ph ph-house"></i>
        <span>回到首页</span>
      </button>

      <!-- 底部装饰 -->
      <div class="bottom-decoration">
        <span class="decoration-dot"></span>
        <span class="decoration-dot"></span>
        <span class="decoration-dot"></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Archivo:wght@400;600;700;900&family=Karla:wght@400;500;600&display=swap');

/* === 主容器 === */
.not-found-wrapper {
  position: fixed;
  top: 32px; /* 为顶部导航栏留出空间 */
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: 'Karla', sans-serif;
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  z-index: 100;
}

/* 暗色模式 */
@media (prefers-color-scheme: dark) {
  .not-found-wrapper {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  }
}

/* === 背景层 === */
.background-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 1;
}

.gradient-mesh {
  position: absolute;
  inset: -50%;
  background:
      radial-gradient(circle at 20% 30%, rgba(255, 183, 195, 0.3) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(167, 216, 255, 0.3) 0%, transparent 50%),
      radial-gradient(circle at 50% 50%, rgba(255, 236, 179, 0.2) 0%, transparent 50%);
  animation: meshFloat 20s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .gradient-mesh {
    background:
        radial-gradient(circle at 20% 30%, rgba(139, 92, 246, 0.15) 0%, transparent 50%),
        radial-gradient(circle at 80% 70%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
        radial-gradient(circle at 50% 50%, rgba(236, 72, 153, 0.1) 0%, transparent 50%);
  }
}

.floating-orb {
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.05));
  filter: blur(40px);
  animation: orbFloat 10s ease-in-out infinite;
  pointer-events: none;
}

@media (prefers-color-scheme: dark) {
  .floating-orb {
    background: radial-gradient(circle at 30% 30%, rgba(139, 92, 246, 0.2), rgba(59, 130, 246, 0.05));
  }
}

.noise-texture {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  opacity: 0.03;
  mix-blend-mode: overlay;
  pointer-events: none;
}

/* === 主卡片 === */
.content-card {
  position: relative;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  border-radius: 48px;
  padding: 60px 50px 50px;
  box-shadow:
      0 20px 60px rgba(0, 0, 0, 0.08),
      0 0 1px rgba(0, 0, 0, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.6);
  max-width: 550px;
  width: 90%;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: cardEntrance 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 100;
}

@media (prefers-color-scheme: dark) {
  .content-card {
    background: rgba(30, 30, 46, 0.8);
    box-shadow:
        0 20px 60px rgba(0, 0, 0, 0.4),
        0 0 1px rgba(255, 255, 255, 0.1),
        inset 0 1px 0 rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.card-glow {
  position: absolute;
  inset: -2px;
  background: linear-gradient(135deg, #ffd1dc, #a7d8ff, #ffe8b3);
  border-radius: 48px;
  opacity: 0.3;
  filter: blur(20px);
  z-index: -1;
  animation: glowPulse 4s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .card-glow {
    background: linear-gradient(135deg, #8b5cf6, #3b82f6, #ec4899);
    opacity: 0.2;
  }
}

/* === 404 数字 === */
.number-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 30px;
}

.number-digit {
  font-family: 'Archivo', sans-serif;
  font-size: 120px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, #ff6b9d 0%, #ffa06e 50%, #ffd93d 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 4px 12px rgba(255, 107, 157, 0.3));
  animation: digitFloat 3s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .number-digit {
    background: linear-gradient(135deg, #c084fc 0%, #60a5fa 50%, #f472b6 100%);
    filter: drop-shadow(0 4px 12px rgba(192, 132, 252, 0.4));
  }
}

.number-zero {
  font-family: 'Archivo', sans-serif;
  font-size: 120px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, #ff6b9d 0%, #ffa06e 50%, #ffd93d 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 4px 12px rgba(255, 107, 157, 0.3));
  animation: digitFloat 3s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .number-zero {
    background: linear-gradient(135deg, #c084fc 0%, #60a5fa 50%, #f472b6 100%);
    filter: drop-shadow(0 4px 12px rgba(192, 132, 252, 0.4));
  }
}

/* === 装饰线条 === */
.decorative-line {
  width: 120px;
  height: 4px;
  margin: 0 auto 30px;
  background: linear-gradient(90deg, transparent, #ff6b9d, #ffa06e, #ffd93d, transparent);
  border-radius: 2px;
  animation: lineGlow 2s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .decorative-line {
    background: linear-gradient(90deg, transparent, #c084fc, #60a5fa, #f472b6, transparent);
  }
}

/* === 文字内容 === */
.text-content {
  text-align: center;
  margin-bottom: 35px;
}

.main-message {
  font-family: 'Archivo', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: #2d2d2d;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
  animation: textFadeIn 0.6s ease-out 0.3s both;
}

@media (prefers-color-scheme: dark) {
  .main-message {
    color: #f5f5f5;
  }
}

.sub-message {
  font-size: 16px;
  line-height: 1.7;
  color: #666;
  margin: 0;
  font-weight: 400;
  animation: textFadeIn 0.6s ease-out 0.4s both;
}

@media (prefers-color-scheme: dark) {
  .sub-message {
    color: #a1a1aa;
  }
}

/* === 返回按钮 === */
.home-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 16px 32px;
  border: none;
  border-radius: 20px;
  font-family: 'Karla', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ff6b9d 0%, #ffa06e 100%);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: buttonFadeIn 0.6s ease-out 0.5s both;
  z-index: 10;
}

@media (prefers-color-scheme: dark) {
  .home-button {
    background: linear-gradient(135deg, #c084fc 0%, #60a5fa 100%);
  }
}

.home-button i {
  font-size: 20px;
}

.home-button:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 12px 24px rgba(255, 107, 157, 0.3);
}

@media (prefers-color-scheme: dark) {
  .home-button:hover {
    box-shadow: 0 12px 24px rgba(192, 132, 252, 0.3);
  }
}

.home-button:active {
  transform: translateY(0) scale(1);
}

/* === 底部装饰 === */
.bottom-decoration {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 30px;
  animation: dotsFadeIn 0.6s ease-out 0.6s both;
}

.decoration-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #ffa06e);
  animation: dotPulse 2s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .decoration-dot {
    background: linear-gradient(135deg, #c084fc, #60a5fa);
  }
}

.decoration-dot:nth-child(2) {
  animation-delay: 0.3s;
}

.decoration-dot:nth-child(3) {
  animation-delay: 0.6s;
}

/* === 动画定义 === */
@keyframes cardEntrance {
  0% {
    opacity: 0;
    transform: scale(0.9) translateY(30px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes digitFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  25% {
    transform: translateY(-8px) rotate(-1deg);
  }
  75% {
    transform: translateY(-4px) rotate(1deg);
  }
}

@keyframes lineGlow {
  0%, 100% {
    opacity: 1;
    filter: blur(0px);
  }
  50% {
    opacity: 0.7;
    filter: blur(1px);
  }
}

@keyframes glowPulse {
  0%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.05);
  }
}

@keyframes meshFloat {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(5deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(-5deg);
  }
}

@keyframes orbFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(20px, -20px) scale(1.1);
  }
}

@keyframes textFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes buttonFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes dotsFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes dotPulse {
  0%, 100% {
    opacity: 0.4;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.3);
  }
}

/* === 响应式设计 === */
@media (max-width: 768px) {
  .content-card {
    padding: 50px 35px 40px;
    border-radius: 36px;
  }

  .number-digit,
  .number-zero {
    font-size: 90px;
  }

  .main-message {
    font-size: 26px;
  }

  .sub-message {
    font-size: 15px;
  }

  .home-button {
    padding: 14px 28px;
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  .number-digit,
  .number-zero {
    font-size: 70px;
  }

  .main-message {
    font-size: 22px;
  }
}
</style>