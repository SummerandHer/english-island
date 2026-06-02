export function questionPromptText(q: {
  direction: string
  promptZh?: string
  promptEn?: string
}): string {
  return q.direction === 'zh2en' ? (q.promptZh || '') : (q.promptEn || '')
}

export function questionPromptPreview(q: {
  direction: string
  promptZh?: string
  promptEn?: string
}, maxLen = 80): string {
  const text = questionPromptText(q).trim().replace(/\s+/g, ' ')
  if (!text) return '（无题干）'
  return text.length > maxLen ? `${text.slice(0, maxLen)}…` : text
}

export function questionAnswerPlaceholder(direction: string): string {
  return direction === 'zh2en' ? '在此输入英文译文…' : '在此输入中文译文…'
}

export function questionDirectionLabel(direction: string): string {
  return direction === 'zh2en' ? '中译英' : '英译中'
}
