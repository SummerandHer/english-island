import type { TranslationGradingError } from '~/types/api'

export function bandTagType(band: string): 'success' | 'info' | 'warning' | 'error' | 'default' {
  if (band.startsWith('13')) return 'success'
  if (band.startsWith('10')) return 'info'
  if (band.startsWith('7')) return 'warning'
  if (band === '0') return 'default'
  return 'error'
}

export function formatSubmissionTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** 在用户译文中高亮 AI 指出的问题片段 */
export function highlightAnswerErrors(userAnswer: string, errors: TranslationGradingError[]): string {
  if (!userAnswer || !errors?.length) {
    return escapeHtml(userAnswer)
  }

  const spans = errors
    .map(e => e.span?.trim())
    .filter((s): s is string => !!s && userAnswer.includes(s))
    .sort((a, b) => b.length - a.length)

  if (!spans.length) {
    return escapeHtml(userAnswer)
  }

  let html = escapeHtml(userAnswer)
  for (const span of spans) {
    const escapedSpan = escapeHtml(span)
    html = html.replace(
      escapedSpan,
      `<mark class="translation-error-mark">${escapedSpan}</mark>`
    )
  }
  return html
}
