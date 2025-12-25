import { ref, onMounted, onUnmounted } from 'vue';

export const useTime = () => {
  const timeStr = ref('');

  const updateTime = () => {
    const date = new Date();
    const options: Intl.DateTimeFormatOptions = {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric'
    };
    timeStr.value = date.toLocaleString('zh-CN', options);
  };

  let timer: number | null = null;

  const startTimer = () => {
    updateTime(); // Initial update
    timer = window.setInterval(updateTime, 1000);
  };

  const stopTimer = () => {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  };

  onMounted(startTimer);
  onUnmounted(stopTimer);

  return {
    timeStr
  };
};
