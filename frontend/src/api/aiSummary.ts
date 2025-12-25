export type GeneratePostSummaryRequest = {
  postId?: number | string;
  title?: string;
  content?: string;
};

export type GeneratePostSummaryResponse = {
  summary: string;
};

export async function generatePostSummary(payload: GeneratePostSummaryRequest): Promise<string> {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 20_000);

  try {
    const resp = await fetch('/api/ai/summary', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });

    if (!resp.ok) {
      const text = await resp.text().catch(() => '');
      throw new Error(text || `AI 摘要接口错误 (${resp.status})`);
    }

    const data = (await resp.json()) as GeneratePostSummaryResponse;
    if (!data?.summary || typeof data.summary !== 'string') {
      throw new Error('AI 摘要接口返回格式不正确');
    }

    return data.summary;
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new Error('AI 摘要请求超时，请稍后重试');
    }
    throw err;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

