/** 轻量 Markdown 渲染：代码块/行内代码/加粗/标题/列表/换行（内部工具够用，不引第三方依赖） */
export function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

export function renderMd(text) {
  if (!text) return ''
  const blocks = []
  let src = escapeHtml(text)
  // 代码块
  src = src.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    blocks.push(`<pre class="code-block"><code>${code.replace(/\n$/, '')}</code></pre>`)
    return `\u0000${blocks.length - 1}\u0000`
  })
  src = src
    .replace(/`([^`\n]+)`/g, '<code class="inline-code">$1</code>')
    .replace(/\*\*([^*\n]+)\*\*/g, '<b>$1</b>')
    .replace(/^###\s+(.+)$/gm, '<div class="md-h">$1</div>')
    .replace(/^##\s+(.+)$/gm, '<div class="md-h">$1</div>')
    .replace(/^#\s+(.+)$/gm, '<div class="md-h">$1</div>')
    .replace(/^\s*[-*]\s+(.+)$/gm, '<div class="md-li">• $1</div>')
    .replace(/^\s*\d+\.\s+(.+)$/gm, '<div class="md-li">$1</div>')
    .replace(/\n/g, '<br/>')
  return src.replace(/\u0000(\d+)\u0000/g, (_, i) => blocks[Number(i)])
}
