// Markdown to HTML converter for Super Market docs
const fs = require('fs');
const path = require('path');

const docsDir = path.resolve(__dirname, '..');
const outDir = __dirname;

// ============================================================
// Syntax Highlighting (single-pass tokenizer per language)
// ============================================================

function highlightGeneric(code, patterns) {
  // Escaped code with markers to protect from re-processing
  let html = code;
  const markers = [];

  // Step 1: Find all matches for all patterns, sorted by position
  const allMatches = [];
  for (const { regex, cls } of patterns) {
    let m;
    while ((m = regex.exec(html)) !== null) {
      allMatches.push({ start: m.index, end: m.index + m[0].length, cls, text: m[0] });
    }
  }

  // Step 2: Sort by start position, longest first for ties
  allMatches.sort((a, b) => a.start - b.start || b.end - a.end);

  // Step 3: Remove overlapping matches (keep first/longest)
  const keep = [];
  let lastEnd = 0;
  for (const m of allMatches) {
    if (m.start >= lastEnd) {
      keep.push(m);
      lastEnd = m.end;
    }
  }

  // Step 4: Build output with highlights
  let result = '';
  let pos = 0;
  for (const m of keep) {
    result += escapeHtml(html.slice(pos, m.start));
    result += `<span class="${m.cls}">${escapeHtml(m.text)}</span>`;
    pos = m.end;
  }
  result += escapeHtml(html.slice(pos));
  return result;
}

const JAVA_PATTERNS = [
  { regex: /\/\/.*$/gm, cls: 'tok-cmt' },
  { regex: /\/\*[\s\S]*?\*\//g, cls: 'tok-cmt' },
  { regex: /"(?:[^"\\]|\\.)*"/g, cls: 'tok-str' },
  { regex: /'\w'/g, cls: 'tok-str' },
  { regex: /@\w+/g, cls: 'tok-ann' },
  { regex: /\b\d+\.?\d*[LfFdD]?\b/g, cls: 'tok-num' },
  { regex: /\b(public|private|protected|static|final|abstract|class|interface|enum|extends|implements|new|return|if|else|for|while|do|switch|case|break|continue|throw|throws|try|catch|finally|import|package|void|int|long|double|float|boolean|char|byte|short|true|false|null|this|super|instanceof|synchronized|volatile|transient|native|strictfp|assert|default|yield|var|record|sealed|permits)\b/g, cls: 'tok-kw' },
  { regex: /\b(String|Integer|Long|Double|Float|Boolean|BigDecimal|LocalDateTime|List|Map|Set|Optional|Stream|Object|Class|Override|Deprecated|SuppressWarnings)\b/g, cls: 'tok-typ' },
];

const SQL_PATTERNS = [
  { regex: /--.*$/gm, cls: 'tok-cmt' },
  { regex: /'(?:[^'\\]|\\.)*'/g, cls: 'tok-str' },
  { regex: /\b\d+\b/g, cls: 'tok-num' },
  { regex: /\b(CREATE|ALTER|DROP|INSERT|UPDATE|DELETE|SELECT|FROM|WHERE|JOIN|LEFT|RIGHT|INNER|OUTER|ON|AND|OR|NOT|IN|EXISTS|BETWEEN|LIKE|ORDER|BY|GROUP|HAVING|LIMIT|OFFSET|UNION|ALL|AS|SET|INTO|VALUES|TABLE|INDEX|VIEW|DATABASE|SCHEMA|IF|PRIMARY|KEY|FOREIGN|REFERENCES|UNIQUE|DEFAULT|AUTO_INCREMENT|COMMENT|ENGINE|CHARSET|COLLATE|INT|BIGINT|VARCHAR|TEXT|DECIMAL|DATETIME|TIMESTAMP|TINYINT|JSON|LONGBLOB|ENUM|USE|GRANT|FLUSH|PRIVILEGES|TO|IDENTIFIED|USER|NOT|CHARACTER|utf8mb4|BY|IF|EXISTS|ON|ON\s+UPDATE|CURRENT_TIMESTAMP)\b/gi, cls: 'tok-kw' },
];

const YAML_PATTERNS = [
  { regex: /#.*$/gm, cls: 'tok-cmt' },
  { regex: /"[^"]*"/g, cls: 'tok-str' },
  { regex: /'[^']*'/g, cls: 'tok-str' },
  { regex: /\$\{[\w.:-]+\}/g, cls: 'tok-var' },
  { regex: /\b(true|false|null|yes|no)\b/g, cls: 'tok-kw' },
  { regex: /\b\d+\.?\d*\b/g, cls: 'tok-num' },
  { regex: /^(\s*)([\w.-]+)(\s*:)/gm, cls: 'tok-prop' },
];

const XML_PATTERNS = [
  { regex: /<!--[\s\S]*?-->/g, cls: 'tok-cmt' },
  { regex: /"[^"]*"/g, cls: 'tok-str' },
  { regex: /<\/?[\w.-]+/g, cls: 'tok-tag' },
];

const DOCKERFILE_PATTERNS = [
  { regex: /#.*$/gm, cls: 'tok-cmt' },
  { regex: /"[^"]*"/g, cls: 'tok-str' },
  { regex: /\$\{\w+\}/g, cls: 'tok-var' },
  { regex: /^(FROM|RUN|CMD|ENTRYPOINT|COPY|ADD|WORKDIR|EXPOSE|ENV|ARG|LABEL|USER|VOLUME|HEALTHCHECK|ONBUILD|SHELL|STOPSIGNAL)\b/gm, cls: 'tok-kw' },
];

const BASH_PATTERNS = [
  { regex: /#.*$/gm, cls: 'tok-cmt' },
  { regex: /"(?:[^"\\]|\\.)*"/g, cls: 'tok-str' },
  { regex: /'(?:[^'\\]|\\.)*'/g, cls: 'tok-str' },
  { regex: /\$\{?\w+\}?/g, cls: 'tok-var' },
  { regex: /\b(echo|cd|ls|mkdir|rm|cp|mv|cat|grep|find|sed|awk|docker|git|mvn|java|javac|curl|wget|chmod|chown|export|source|for|do|done|if|then|else|fi|while|case|esac|function|return|exit|sleep|bash|sh|set|unset|env|exec|kill|ps|tar|gzip|unzip|npm|npx|node|python|pip|yarn)\b/g, cls: 'tok-builtin' },
];

const PROPS_PATTERNS = [
  { regex: /#.*$/gm, cls: 'tok-cmt' },
  { regex: /\b\d+\b/g, cls: 'tok-num' },
];

const HIGHLIGHTERS = {
  java:       (c) => highlightGeneric(c, JAVA_PATTERNS),
  sql:        (c) => highlightGeneric(c, SQL_PATTERNS),
  yaml:       (c) => highlightGeneric(c, YAML_PATTERNS),
  xml:        (c) => highlightGeneric(c, XML_PATTERNS),
  dockerfile: (c) => highlightGeneric(c, DOCKERFILE_PATTERNS),
  bash:       (c) => highlightGeneric(c, BASH_PATTERNS),
  sh:         (c) => highlightGeneric(c, BASH_PATTERNS),
  properties: (c) => highlightGeneric(c, PROPS_PATTERNS),
  env:        (c) => highlightGeneric(c, PROPS_PATTERNS),
  json:       (c) => highlightGeneric(c, [{ regex: /"(?:[^"\\]|\\.)*"/g, cls: 'tok-str' }, { regex: /\b\d+\.?\d*\b/g, cls: 'tok-num' }, { regex: /\b(true|false|null)\b/g, cls: 'tok-kw' }]),
};

function highlightCode(code, lang) {
  const fn = HIGHLIGHTERS[lang];
  if (fn) {
    try { return fn(code); } catch(e) { /* fallback */ }
  }
  return escapeHtml(code);
}

// ============================================================
// Diagram Builder Helpers
// ============================================================

function wrap(tag, cls, inner) { return `<div class="${cls}">${inner}</div>`; }

function layerStack(layers) {
  const html = layers.map(l => {
    const nodes = l.items ? `<div class="arch-items">${l.items.map(s => `<span class="arch-item">${s}</span>`).join('')}</div>` : '';
    return `<div class="layer layer-${l.style}">${l.label}${nodes}</div>`;
  }).join('');
  return wrap('div', 'diagram layer-stack', html);
}

function flowChart(nodes) {
  const html = nodes.map((n, i) => {
    let h = `<div class="flow-node ${n.type || ''}">${n.label}</div>`;
    if (i < nodes.length - 1) h += `<span class="flow-arrow">→</span>`;
    return h;
  }).join('');
  return wrap('div', 'diagram flow', html);
}

function compGrid(items) {
  const html = items.map(c => `<div class="comp-box ${c.style || ''}">${c.label}</div>`).join('');
  return wrap('div', 'diagram comp-grid', html);
}

function svcMesh(rows) {
  const html = rows.map(r => {
    const chips = r.items.map(s => `<span class="svc-chip ${s.style || ''}">${s.label}</span>`).join('');
    return `<div class="svc-row">${chips}</div>`;
  }).join('');
  return wrap('div', 'diagram svc-mesh', html);
}

function tree(root, branches) {
  let html = `<div class="tree-root">${root}</div><div class="tree-branches">`;
  for (const b of branches) {
    html += `<div class="tree-branch"><div class="tree-label">${b.label}</div>`;
    for (const l of b.leaves) html += `<div class="tree-leaf">${l}</div>`;
    html += '</div>';
  }
  html += '</div>';
  return wrap('div', 'diagram tree', html);
}

function timeline(phases) {
  const html = phases.map(p =>
    `<div class="timeline-phase ${p.status || ''}">
      <span class="phase-num">${p.name}</span><span class="phase-date">${p.date || ''}</span>
      <div class="phase-title">${p.title}</div>
      <div class="phase-detail">${p.detail || ''}</div>
    </div>`
  ).join('');
  return wrap('div', 'diagram timeline', html);
}

function archOverview(layers) {
  const html = layers.map(l =>
    `<div class="arch-layer" style="background:${l.bg || '#fff'};border-color:${l.bd || '#e5e7eb'}">
      <div class="arch-label" style="color:${l.fg || '#333'}">${l.label}</div>
      ${l.items ? `<div class="arch-items">${l.items.map(s => `<span class="arch-item">${s}</span>`).join('')}</div>` : ''}
    </div>`
  ).join('');
  return wrap('div', 'diagram arch', html);
}

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ============================================================
// Markdown to HTML
// ============================================================

function md2html(md) {
  const lines = md.split('\n');
  const out = [];
  let i = 0;
  let inCodeBlock = false, codeLang = '', codeLines = [];
  let diagramIdx = 0;
  const diagrams = [];

  function inlineFormat(text) {
    text = text.replace(/`([^`]+)`/g, '<code>$1</code>');
    text = text.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
    text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    text = text.replace(/\*(.+?)\*/g, '<em>$1</em>');
    text = text.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1">');
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
    return text;
  }

  function tableToHtml(rows) {
    const dataRows = rows.filter(r => !/^\|[\s\-:|]+\|$/.test(r));
    if (!dataRows.length) return '';
    let html = '<table>\n';
    dataRows.forEach((row, idx) => {
      const cells = row.split('|').filter(c => c.length > 0).map(c => c.trim());
      const tag = idx === 0 ? 'th' : 'td';
      html += '<tr>\n' + cells.map(c => `  <${tag}>${inlineFormat(c)}</${tag}>`).join('\n') + '\n</tr>\n';
    });
    html += '</table>';
    return html;
  }

  function isTableLine(line) { return line.includes('|') && line.trim().startsWith('|'); }
  function isDiagramLine(line) { const t = line.trim(); return t && /[┌└├┤┬┴┼│─╭╰╯▁▔▏▎▍▌▋▊▉█▄▀▐░▒▓]/.test(t); }

  function collectParagraph(startIdx) {
    const pLines = []; let j = startIdx;
    while (j < lines.length) {
      const l = lines[j];
      if (l.trim() === '') break;
      if (l.startsWith('```')) break;
      if (/^#{1,6}\s/.test(l)) break;
      if (isTableLine(l)) break;
      if (/^\s*[-*]\s/.test(l)) break;
      if (/^\s*\d+\.\s/.test(l)) break;
      if (l.startsWith('>')) break;
      if (/^---\s*$/.test(l.trim())) break;
      if (isDiagramLine(l)) break;
      pLines.push(l); j++;
    }
    return { lines: pLines, endIdx: j };
  }

  while (i < lines.length) {
    const line = lines[i]; const trimmed = line.trim();

    if (inCodeBlock) {
      if (trimmed.startsWith('```')) {
        const content = codeLines.join('\n');
        if (!codeLang && /[┌└├┤┬┴┼│─]/.test(content)) {
          const idx = diagramIdx++; diagrams.push(content);
          out.push(`<!--DIAGRAM:${idx}-->`);
        } else {
          const highlighted = highlightCode(content, codeLang);
          out.push(`<pre data-lang="${codeLang || 'text'}"><code class="language-${codeLang}">${highlighted}</code></pre>`);
        }
        inCodeBlock = false; codeLines = []; codeLang = '';
      } else {
        codeLines.push(line);
      }
      i++; continue;
    }

    if (trimmed.startsWith('```')) { inCodeBlock = true; codeLang = trimmed.slice(3).trim(); i++; continue; }
    if (trimmed === '') { i++; continue; }

    if (/^#{1,6}\s/.test(trimmed)) {
      const m = trimmed.match(/^(#{1,6})\s+(.+)/);
      out.push(`<h${m[1].length}>${inlineFormat(m[2])}</h${m[1].length}>`);
      i++; continue;
    }

    if (/^---\s*$/.test(trimmed) || /^\*\*\*+\s*$/.test(trimmed)) { out.push('<hr>'); i++; continue; }

    if (isTableLine(line)) {
      const tableRows = [];
      while (i < lines.length && isTableLine(lines[i])) { tableRows.push(lines[i].trim()); i++; }
      out.push(tableToHtml(tableRows)); continue;
    }

    if (/^\s*[-*]\s/.test(line)) {
      out.push('<ul>');
      while (i < lines.length && /^\s*[-*]\s/.test(lines[i])) { out.push(`<li>${inlineFormat(lines[i].replace(/^\s*[-*]\s+/, ''))}</li>`); i++; }
      out.push('</ul>'); continue;
    }

    if (/^\s*\d+\.\s/.test(line)) {
      out.push('<ol>');
      while (i < lines.length && /^\s*\d+\.\s/.test(lines[i])) { out.push(`<li>${inlineFormat(lines[i].replace(/^\s*\d+\.\s+/, ''))}</li>`); i++; }
      out.push('</ol>'); continue;
    }

    if (line.startsWith('>')) {
      out.push('<blockquote>');
      while (i < lines.length && lines[i].startsWith('>')) { out.push(inlineFormat(lines[i].replace(/^>\s?/, ''))); i++; }
      out.push('</blockquote>'); continue;
    }

    if (isDiagramLine(line)) {
      const diagLines = [];
      while (i < lines.length && (isDiagramLine(lines[i]) || (lines[i].trim() === '' && i+1 < lines.length && isDiagramLine(lines[i+1])))) {
        if (lines[i].trim() !== '') diagLines.push(lines[i]);
        i++;
      }
      const idx = diagramIdx++; diagrams.push(diagLines.join('\n'));
      out.push(`<!--DIAGRAM:${idx}-->`); continue;
    }

    const para = collectParagraph(i);
    if (para.lines.length > 0) { out.push(`<p>${para.lines.map(l => inlineFormat(l)).join('\n')}</p>`); i = para.endIdx; }
    else { i++; }
  }

  if (inCodeBlock) {
    const content = codeLines.join('\n');
    const highlighted = highlightCode(content, codeLang);
    out.push(`<pre data-lang="${codeLang || 'text'}"><code>${highlighted}</code></pre>`);
  }

  return { html: out.join('\n'), diagrams };
}

// ============================================================
// Diagram Recognition
// ============================================================

function matchDiagram(diagText) {
  // A: Seven-layer architecture
  if (diagText.includes('客户端层') && diagText.includes('基础设施层')) {
    return layerStack([
      { label: '客户端层 — PC用户前台、商家后台、运营后台（Vue3 + Element Plus）', style: 'client', items: ['Vue3 + Vite', 'Element Plus', 'Pinia', 'Axios'] },
      { label: '接入层 — 开发：localhost:Nginx；生产：MetalLB+Nginx Ingress+WAF', style: 'access', items: ['Nginx', 'MetalLB', 'ModSecurity'] },
      { label: '网关层 — Spring Cloud Gateway集群、统一认证鉴权、限流熔断、链路追踪', style: 'gateway', items: ['鉴权认证', '限流熔断', '请求路由', '协议转换', '链路追踪'] },
      { label: '业务服务层 — Dubbo 3.x微服务集群（用户、商品、订单、支付、营销等）', style: 'business', items: ['用户服务', '商品服务', '订单服务', '支付服务', '营销服务', '搜索服务', '商家服务', '平台服务'] },
      { label: '中间件层 — 消息队列、分布式缓存、搜索引擎、分布式事务、分布式锁', style: 'middleware', items: ['RocketMQ', 'Redis', 'ES', 'Seata', 'Redisson', 'Sentinel'] },
      { label: '数据存储层 — 关系型数据库（分库分表）、NoSQL、对象存储、数据仓库', style: 'storage', items: ['MySQL 8.0', 'MongoDB', 'MinIO', 'ClickHouse', 'ShardingSphere'] },
      { label: '基础设施层 — 开发：Docker Compose；生产：本地K8s集群+GitLab CI/CD', style: 'infra', items: ['Docker', 'K8s', 'GitLab CI/CD', 'Prometheus', 'Grafana'] },
    ]);
  }
  // B: Dev deployment
  if (diagText.includes('开发者本地机器') && diagText.includes('前端项目(Vite)')) {
    return archOverview([
      { label: '开发者本地机器', bg: '#f0f9ff', bd: '#93c5fd', fg: '#1e40af', items: ['前端项目(Vite) → 本地Nginx → Spring Cloud Gateway', 'Dubbo微服务集群(IDE启动) → Docker Compose中间件集群', 'Nacos · MySQL · Redis · RocketMQ · ES · MinIO'] },
    ]);
  }
  // C: Production deployment
  if (diagText.includes('本地K8s集群入口') && diagText.includes('MetalLB')) {
    return layerStack([
      { label: '本地K8s集群入口 — MetalLB(四层LB) → Nginx Ingress → ModSecurity WAF', style: 'access', items: ['DNS', 'MetalLB', 'Nginx Ingress', 'WAF'] },
      { label: '业务应用层 — Spring Cloud Gateway(3节点) + Dubbo微服务集群(各3-5节点)', style: 'gateway', items: ['Gateway ×3', '18 微服务各3-5节点'] },
      { label: '中间件集群 — Redis Cluster(3主3从) · RocketMQ(3节点) · ES(3节点)', style: 'middleware', items: ['Redis Cluster', 'RocketMQ ×3', 'ES ×3', 'Nacos ×3', 'Sentinel ×3', 'SkyWalking ×3'] },
      { label: '数据存储集群 — MySQL(8主16从) · MongoDB(3节点) · MinIO(4节点) · ClickHouse(3节点)', style: 'storage', items: ['MySQL 8主16从', 'MongoDB ×3', 'MinIO ×4', 'ClickHouse ×3', 'Harbor ×3'] },
      { label: '基础设施层 — GitLab(3节点) · GitLab Runner(动态伸缩) · Prometheus(3节点) · Grafana · Kibana', style: 'infra', items: ['GitLab ×3', 'GitLab Runner', 'Prometheus ×3', 'Grafana', 'Kibana', 'Dubbo Admin'] },
    ]);
  }
  // D: User roles tree
  if (diagText.includes('系统用户体系') && diagText.includes('C端用户')) {
    return tree('系统用户体系', [
      { label: 'C端用户', leaves: ['注册/登录', '浏览商品', '下单支付', '评价商品', '售后申请'] },
      { label: 'B端商家', leaves: ['店铺管理', '商品上架', '订单处理', '营销活动', '财务结算'] },
      { label: '平台运营', leaves: ['商家审核', '类目管理', '活动配置', '风控审核', '数据分析'] },
    ]);
  }
  // E: System architecture overview
  if (diagText.includes('客户端层 (Client)') && diagText.includes('接入层 (Access)')) {
    return archOverview([
      { label: '客户端层 (Client) — 用户前台 B2C · 商家后台 B2B · 运营后台 Admin', bg: '#dbeafe', bd: '#93c5fd', fg: '#1e40af', items: ['用户前台 (B2C)', '商家后台 (B2B)', '运营后台 (Admin)'] },
      { label: '接入层 (Access) — Nginx 反向代理 + 静态资源 + gzip · 生产: MetalLB + Ingress + WAF', bg: '#e0e7ff', bd: '#a5b4fc', fg: '#3730a3', items: ['Nginx', 'MetalLB', 'ModSecurity WAF'] },
      { label: '网关层 (Gateway) — Spring Cloud Gateway 集群 · JWT验签 · Sentinel限流 · HTTP→Dubbo协议转换 · SkyWalking追踪', bg: '#ede9fe', bd: '#c4b5fd', fg: '#6b21a8', items: ['鉴权认证', '限流熔断', '请求路由', '协议转换', '链路追踪'] },
      { label: '业务服务层 (Business) — Dubbo 3.x Triple Protocol · Nacos 注册中心 · 18个微服务', bg: '#d1fae5', bd: '#6ee7b7', fg: '#065f46', items: ['用户服务', '商品服务', '订单服务', '支付服务', '购物车', '商家店铺', '优惠券', '搜索服务', '文件服务', '通知服务', '运营平台'] },
      { label: '中间件层 (Middleware) — RocketMQ · Redis · ES · Seata · Sentinel · XXL-Job · SkyWalking · Redisson', bg: '#fef3c7', bd: '#fcd34d', fg: '#92400e', items: ['RocketMQ 消息队列', 'Redis 分布式缓存', 'ES 搜索引擎', 'Seata 分布式事务', 'Sentinel 熔断降级', 'XXL-Job 任务调度', 'SkyWalking 链路追踪', 'Redisson 分布式锁'] },
      { label: '数据存储层 (Storage) — MySQL · MongoDB · MinIO · ClickHouse · 分库分表: ShardingSphere · 数据同步: Canal → ES/ClickHouse', bg: '#ffe4e6', bd: '#fda4af', fg: '#9b2c2c', items: ['MySQL 8.0', 'MongoDB 7.0', 'MinIO 对象存储', 'ClickHouse 数据仓库', 'ShardingSphere 分库分表', 'Canal 数据同步'] },
    ]);
  }
  // F: Order flow
  if (diagText.includes('用户浏览商品') && diagText.includes('加入购物车')) {
    return flowChart([
      { type: 'start', label: '用户浏览商品' }, { label: '加入购物车' },
      { label: '确认订单\n(地址/优惠券/支付方式)' }, { label: '提交订单\n→ 订单服务预创建' },
      { type: 'event', label: '库存扣减\n优惠券核销\n积分计算\n(Seata AT事务)' },
      { label: '订单→待付款' }, { label: '支付服务发起支付' },
      { type: 'decision', label: '支付成功?' }, { type: 'end', label: '已取消\n(30min超时)' },
      { type: 'success', label: '已完成\n通知发货' },
    ]);
  }
  // G: Product listing flow
  if (diagText.includes('商家创建商品') && diagText.includes('提交审核')) {
    return flowChart([
      { type: 'start', label: '商家创建\n商品(草稿)' }, { label: '填写SPU信息\n+ SKU规格\n+ 图片上传' },
      { label: '提交审核' }, { type: 'decision', label: '平台审核' },
      { type: 'end', label: '驳回\n通知商家修改' }, { label: '商品服务\n持久化SPU/SKU' },
      { label: '库存服务\n初始化库存' }, { type: 'event', label: 'RocketMQ\n事务消息' },
      { label: 'Canal监听\nbinlog变化' }, { label: '索引同步\n更新ES' },
      { type: 'success', label: '商品可搜索\n可购买 ✓' },
    ]);
  }
  // H: Refund flow
  if (diagText.includes('用户申请退款') && (diagText.includes('仅退款') || diagText.includes('退货退款'))) {
    return flowChart([
      { type: 'start', label: '用户申请退款' }, { label: '退款服务\n创建退款单' },
      { type: 'decision', label: '是否已发货?' }, { label: '仅退款\n(未发货)' },
      { label: '退货退款\n(已发货)' }, { label: '商家审核通过' },
      { label: '用户退货\n+ 填物流' }, { label: '退款服务退款' },
      { label: '商家确认收货' }, { label: '支付回调处理' },
      { type: 'event', label: '订单状态→已退款\n库存回补' },
    ]);
  }
  // I: Multi-level cache
  if (diagText.includes('L1: 本地缓存') || (diagText.includes('Caffeine') && diagText.includes('L2'))) {
    return archOverview([
      { label: '请求进入', bg: '#f0f9ff', bd: '#93c5fd', fg: '#1e40af' },
      { label: 'L1: 本地缓存 (Caffeine) — 容量1000条 · TTL 60s · 命中率目标 80%+ — 类目树、字典配置、热点商品基础信息', bg: '#d1fae5', bd: '#6ee7b7', fg: '#065f46', items: ['Caffeine 本地缓存', '1000条 · 60s TTL'] },
      { label: 'L2: 分布式缓存 (Redis 7.2) — 容量32GB · TTL按业务场景 · 命中率目标 95%+ — 用户Session、商品详情、库存、购物车、热点数据', bg: '#fef3c7', bd: '#fcd34d', fg: '#92400e', items: ['Redis 7.2', '32GB · 按场景TTL'] },
      { label: 'L3: 数据库 (MySQL) — 最后兜底 · 查询后回写 L1 + L2', bg: '#ffe4e6', bd: '#fda4af', fg: '#9b2c2c', items: ['MySQL 8.0', '回写 L1 + L2'] },
    ]);
  }
  // J: Auth flow
  if (diagText.includes('用户登录') && (diagText.includes('JWT') || diagText.includes('Token'))) {
    return flowChart([
      { type: 'start', label: '用户登录\nPOST /api/auth/login' }, { label: 'auth-service\n校验密码(BCrypt)' },
      { type: 'decision', label: '校验通过?' }, { type: 'end', label: '返回 401' },
      { type: 'event', label: '生成 Access Token\n(JWT TTL=2h)\n+ Refresh Token\n(TTL=7d)' },
      { label: '返回 Tokens' }, { label: '前端存储\nAccessToken→内存\nRefreshToken→Cookie' },
      { label: '后续请求\nAuthorization:\nBearer {token}' },
      { label: 'Gateway 全局过滤器\nJWT验签(RSA256)\n解析 userId+roles' },
      { label: '透传 Header\nX-User-Id\nX-User-Roles' },
    ]);
  }
  // K: RBAC model
  if ((diagText.includes('用户') && diagText.includes('角色') && diagText.includes('权限') && diagText.includes('Role')) ||
      (diagText.includes('ROLE_USER') && diagText.includes('ROLE_MERCHANT'))) {
    return tree('权限模型 RBAC', [
      { label: '用户 (User)', leaves: ['ROLE_USER 普通用户', 'ROLE_MERCHANT 商家', 'ROLE_ADMIN 运营管理员'] },
      { label: '角色 (Role) — N:M 关联', leaves: ['用户角色绑定', '角色权限绑定', '支持多角色'] },
      { label: '权限 (Permission)', leaves: ['product:read 查看商品', 'product:write 编辑商品', 'product:audit 审核商品', 'order:read 查看订单', 'order:cancel 取消订单'] },
    ]);
  }
  // L: Seckill flow
  if (diagText.includes('秒杀商品预热') || (diagText.includes('秒杀') && diagText.includes('Redis Lua'))) {
    return flowChart([
      { type: 'event', label: 'T-1h 秒杀商品预热\n库存加载到Redis\n商品信息加载到本地缓存' },
      { type: 'event', label: 'T-5min 倒计时\nWebSocket推送' },
      { type: 'start', label: 'T-0 秒杀开始\n用户点击秒杀' },
      { label: 'Gateway限流\nSentinel 令牌桶\n10000 QPS' }, { label: 'Redisson\n分布式锁' },
      { type: 'event', label: 'Redis Lua脚本\n原子扣减秒杀库存' }, { type: 'decision', label: '抢到?' },
      { type: 'end', label: '已抢光' }, { label: '发送RocketMQ\n异步创建订单' },
      { type: 'success', label: 'WebSocket\n推送结果 ✓' },
    ]);
  }
  // M: Seata AT
  if ((diagText.includes('订单服务') && diagText.includes('TM')) || diagText.includes('Seata AT 模式')) {
    return compGrid([
      { label: '订单服务 (TM)\n事务发起方', style: 'client' },
      { label: '库存服务 (RM)\ndeductStock\nSeata代理数据源\nundo_log回滚', style: 'service' },
      { label: '优惠券服务 (RM)\nuseCoupon\nSeata代理数据源', style: 'service' },
      { label: '订单服务 (RM)\ncreateOrder\nINSERT orders\n+ order_items', style: 'service' },
    ]);
  }
  // N: Inventory deduction
  if (diagText.includes('Redis预减') || (diagText.includes('Redis 库存') && diagText.includes('DB 真实扣减'))) {
    return flowChart([
      { type: 'start', label: '检查 Redis 库存\n(> 0)' }, { label: 'Redis 预减库存\n(DECR)' },
      { label: '发送 RocketMQ\n消息' }, { label: '消费端:\nDB 真实扣减\n(乐观锁)' },
      { type: 'decision', label: '扣减成功?' }, { label: '重试\n(最多3次)' },
      { type: 'end', label: 'Redis库存补偿\n+ 告警' }, { type: 'success', label: '完成 ✓' },
    ]);
  }
  // O: DB sharding
  if (diagText.includes('Nacos 配置中心') && diagText.includes('ShardingSphere')) {
    return compGrid([
      { label: 'Nacos 配置中心\n数据源 & 分片规则', style: 'client' },
      { label: 'ShardingSphere\n垂直分库', style: 'service' },
      { label: '读写分离\n一主多从', style: 'mid' },
      { label: '分表策略\n哈希取模', style: 'store' },
    ]);
  }
  // P: Observability
  if (diagText.includes('可观测性三支柱') || (diagText.includes('Metrics') && diagText.includes('Tracing') && diagText.includes('Logging'))) {
    return tree('可观测性三支柱', [
      { label: 'Metrics 指标 — Prometheus + Grafana', leaves: ['服务可用率 < 99.9% → P1', '接口P99 > 500ms → P2', 'Redis内存 > 80% → P2', '消息积压 > 10000 → P1', '订单成功率 < 99% → P0'] },
      { label: 'Tracing 链路追踪 — SkyWalking 9.7', leaves: ['全链路追踪', '服务拓扑图', '性能剖析', '依赖分析'] },
      { label: 'Logging 日志 — ELK Stack', leaves: ['ES 日志存储', 'Kibana 可视化', 'Filebeat 采集', '集中检索'] },
    ]);
  }
  // Q: K8s cluster planning
  if (diagText.includes('集群规划') && (diagText.includes('Master') || diagText.includes('Worker'))) {
    return compGrid([
      { label: 'Master 节点 ×3\n4C8G\netcd · kube-apiserver\nscheduler · controller\nNginx Ingress', style: 'gateway' },
      { label: 'Worker 节点 ×5\n8C16G\n业务服务 (Deployment)\n中间件 (StatefulSet)\n存储 (StatefulSet+PV)', style: 'service' },
      { label: 'MetalLB (Layer 2)\nVIP: 192.168.1.200-250\n暴露: Nginx Ingress\n(80/443)', style: 'access' },
      { label: '存储方案\n开发: hostPath\n生产: Longhorn\n/ Rook Ceph', style: 'store' },
    ]);
  }
  // R: Service list tree
  if (diagText.includes('基础服务 (P0)') || (diagText.includes('user-service') && diagText.includes('├──'))) {
    const groups = [
      { label: '基础服务 (P0)', style: 'service', items: ['用户服务 :9301', '认证授权 :9302', '会员服务 :9303', '地址服务 :9304', '商品服务 :9311', '类目服务 :9312', '库存服务 :9313', '评价服务 :9314', '购物车 :9321', '订单服务 :9322', '支付服务 :9331', '商家店铺 :9341'] },
      { label: '扩展服务 (P1)', style: 'mid', items: ['优惠券服务 :9351', '秒杀服务 :9352', '搜索服务 :9361'] },
      { label: '支撑服务 (P1)', style: 'store', items: ['文件服务 :9371', '通知服务 :9372', '运营平台 :9381'] },
    ];
    const html = groups.map(g => {
      const chips = g.items.map(s => `<span class="svc-chip ${g.style}">${s}</span>`).join('');
      return `<div class="comp-group"><div class="comp-group-title">${g.label}</div><div class="svc-row">${chips}</div></div>`;
    }).join('');
    return wrap('div', 'diagram', html);
  }
  // S: Dev roadmap
  if (diagText.includes('Phase 1 (W1-W2)') && diagText.includes('基础设施搭建')) {
    return timeline([
      { name: 'Phase 1', date: 'W1-W2', title: '基础设施搭建', detail: 'Docker Compose · Maven父工程 · 服务脚手架 · CI/CD', status: 'done' },
      { name: 'Phase 2', date: 'W3-W6', title: '用户+商品域', detail: 'user/auth/member/address/product/category/inventory/shop服务', status: 'done' },
      { name: 'Phase 3', date: 'W7-W10', title: '交易+支付域', detail: 'cart/order/payment服务 · Seata AT · RocketMQ', status: 'done' },
      { name: 'Phase 4', date: 'W11-W13', title: '搜索+营销域', detail: 'search/coupon/seckill服务 · Canal+ES集成', status: '' },
      { name: 'Phase 5', date: 'W14-W16', title: '支撑服务+完善', detail: 'review/notify/file/platform · 监控面板', status: '' },
      { name: 'Phase 6', date: 'W17-W18', title: '压测+优化', detail: 'JMeter · JVM/DB/缓存调优 · 高可用演练', status: '' },
    ]);
  }
  // T: Project directory tree — keep as styled pre block
  if (diagText.includes('super-market/') && (diagText.includes('docs/') || diagText.includes('pom.xml'))) {
    const dirs = diagText.split('\n').filter(l => l.trim()).map(l => l.trim());
    return `<pre class="arch-diagram" style="text-align:left;padding:16px 24px;">${escapeHtml(dirs.join('\n'))}</pre>`;
  }

  return null;
}

function renderDiagrams(html, diagrams) {
  return html.replace(/<!--DIAGRAM:(\d+)-->/g, (match, idx) => {
    const diagText = diagrams[parseInt(idx)];
    if (!diagText) return '';
    const diagHtml = matchDiagram(diagText);
    if (!diagHtml) return `<pre class="arch-diagram">${escapeHtml(diagText)}</pre>`;
    return diagHtml;
  });
}

function md2htmlWithDiagrams(md) {
  const { html, diagrams } = md2html(md);
  return renderDiagrams(html, diagrams);
}

// ============================================================
// Page Template
// ============================================================

function page(title, body, navLabel) {
  const nav = [
    { label: 'Index', href: 'index.html' },
    { label: 'PRD', href: 'prd.html' },
    { label: 'Design', href: 'solution-design.html' },
    { label: 'Services', href: 'service-registry.html' },
    { label: 'P1', href: 'phase1.html' },
    { label: 'P2', href: 'phase2.html' },
    { label: 'P3', href: 'phase3.html' },
    { label: 'P4', href: 'phase4.html' },
  ];
  const navHtml = nav.map(n =>
    `<a href="${n.href}"${n.label === navLabel ? ' class="active"' : ''}>${n.label}</a>`
  ).join('\n');

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${title} — Super Market</title>
<link rel="stylesheet" href="style.css">
</head>
<body>
<nav class="top-nav">
  <div class="nav-inner">
    <a href="index.html" class="logo">Super Market</a>
    <div class="nav-links">${navHtml}</div>
  </div>
</nav>
<main class="container">
${body}
</main>
<footer>
  <p>Generated ${new Date().toISOString().slice(0,10)} — Super Market E-Commerce Platform</p>
</footer>
</body>
</html>`;
}

// ============================================================
// Generate Pages
// ============================================================

// PRD
do {
  const md = fs.readFileSync(path.join(docsDir, 'prd.md'), 'utf-8');
  const html = page('PRD — 产品需求文档', md2htmlWithDiagrams(md), 'PRD');
  fs.writeFileSync(path.join(outDir, 'prd.html'), html, 'utf-8');
  console.log(`Generated: prd.html (${(html.length/1024).toFixed(0)}KB)`);
} while(0);

// Solution Design
do {
  const md = fs.readFileSync(path.join(docsDir, 'solution-design.md'), 'utf-8');
  const html = page('Solution Design — 方案设计', md2htmlWithDiagrams(md), 'Design');
  fs.writeFileSync(path.join(outDir, 'solution-design.html'), html, 'utf-8');
  console.log(`Generated: solution-design.html (${(html.length/1024).toFixed(0)}KB)`);
} while(0);

// Service Registry
do {
  const md = fs.readFileSync(path.join(docsDir, 'superpowers/plans/service-registry.md'), 'utf-8');
  const html = page('Service Registry — 微服务注册表', md2htmlWithDiagrams(md), 'Services');
  fs.writeFileSync(path.join(outDir, 'service-registry.html'), html, 'utf-8');
  console.log('Generated: service-registry.html');
} while(0);

// Individual Phase Pages
const phaseDefs = [
  { file: '2026-05-12-phase1-infrastructure.md', out: 'phase1.html', title: 'Phase 1 — 基础设施搭建与脚手架', nav: 'Phase 1', phaseName: 'Phase 1', phaseRange: 'W1-W2', desc: 'Docker Compose 中间件编排 · Maven 多模块工程 · 18 服务脚手架 · Spring Cloud Gateway · CI/CD · K8s 基础配置' },
  { file: '2026-05-12-phase2-user-product-services.md', out: 'phase2.html', title: 'Phase 2 — 用户域 + 商品域', nav: 'Phase 2', phaseName: 'Phase 2', phaseRange: 'W3-W6', desc: '8 个微服务完整实现 · DDL · Entity · Mapper · Service · Controller · 单元测试 (35+)' },
  { file: '2026-05-12-phase3-order-payment-services.md', out: 'phase3.html', title: 'Phase 3 — 交易域 + 支付域', nav: 'Phase 3', phaseName: 'Phase 3', phaseRange: 'W7-W10', desc: '3 个微服务 · Seata AT 分布式事务 · RocketMQ 消息驱动 · 支付幂等 · 超时取消 (48 测试)' },
  { file: '2026-05-13-phase4-search-marketing-design.md', out: 'phase4.html', title: 'Phase 4 — 搜索域 + 营销域', nav: 'Phase 4', phaseName: 'Phase 4', phaseRange: 'W11-W13', desc: '4 个微服务 · Canal ES 索引同步 · 优惠券(模板+发放+核销) · 秒杀(Redis Lua + MQ异步下单) · 商品评价', dir: 'specs' },
];

for (const p of phaseDefs) {
  const subDir = p.dir || 'plans';
  const md = fs.readFileSync(path.join(docsDir, 'superpowers', subDir, p.file), 'utf-8');
  const body = `<div class="phase-header">
    <span class="phase-badge">${p.phaseName}</span>
    <span class="phase-period">${p.phaseRange}</span>
    <p>${p.desc}</p>
  </div>\n` + md2htmlWithDiagrams(md);
  const html = page(p.title, body, p.nav);
  fs.writeFileSync(path.join(outDir, p.out), html, 'utf-8');
  console.log(`Generated: ${p.out} (${(html.length/1024).toFixed(0)}KB)`);
}

// Index page
const indexBody = `
<h1>Super Market 项目文档</h1>
<p class="subtitle">京东式商城系统 — 基于 Dubbo 3.x 的微服务电商平台</p>

<section class="overview">
  <h2>项目概况</h2>
  <div class="stats">
    <div class="stat-card"><strong>18</strong><span>微服务</span></div>
    <div class="stat-card"><strong>JDK 21</strong><span>运行环境</span></div>
    <div class="stat-card"><strong>Dubbo 3.2</strong><span>RPC 框架</span></div>
    <div class="stat-card"><strong>3 已完成</strong><span>开发阶段</span></div>
  </div>
</section>

<section>
  <h2>核心文档</h2>
  <div class="doc-grid">
    <a href="prd.html" class="doc-card">
      <h3>PRD 产品需求文档</h3>
      <p>业务目标、技术选型、部署架构、开发与生产双环境方案</p>
      <span class="tag">业务架构</span><span class="tag">技术选型</span>
    </a>
    <a href="solution-design.html" class="doc-card">
      <h3>Solution Design 方案设计</h3>
      <p>需求分析、系统架构、数据库设计、缓存/消息队列/安全设计、关键场景技术方案</p>
      <span class="tag">系统架构</span><span class="tag">数据库</span><span class="tag">安全</span>
    </a>
    <a href="service-registry.html" class="doc-card">
      <h3>Service Registry 服务注册表</h3>
      <p>18 个微服务清单：端口、数据库、包名映射关系</p>
      <span class="tag">服务清单</span>
    </a>
  </div>
</section>

<section>
  <h2>开发实施阶段</h2>
  <div class="doc-grid">
    <a href="phase1.html" class="doc-card">
      <h3>Phase 1 — 基础设施搭建</h3>
      <p>Docker Compose 中间件编排 · Maven 多模块工程 · 18 服务脚手架 · Spring Cloud Gateway · CI/CD 流水线 · K8s 基础配置</p>
      <span class="tag">W1-W2</span><span class="tag">完成</span>
    </a>
    <a href="phase2.html" class="doc-card">
      <h3>Phase 2 — 用户域 + 商品域</h3>
      <p>user/auth/member/address/product/category/inventory/shop 共 8 个微服务完整实现，含 DDL、Entity、Service、Controller、单元测试</p>
      <span class="tag">W3-W6</span><span class="tag">35+ 测试</span><span class="tag">完成</span>
    </a>
    <a href="phase3.html" class="doc-card">
      <h3>Phase 3 — 交易域 + 支付域</h3>
      <p>cart/order/payment 3 个微服务，Seata AT 分布式事务、RocketMQ 延迟消息、支付幂等</p>
      <span class="tag">W7-W10</span><span class="tag">14 测试</span><span class="tag">完成</span>
    </a>
    <a href="phase4.html" class="doc-card">
      <h3>Phase 4 — 搜索域 + 营销域</h3>
      <p>search/coupon/seckill/review 4 个微服务，Canal ES 索引同步、优惠券、秒杀 Redis Lua、评价</p>
      <span class="tag">W11-W13</span><span class="tag">设计完成</span>
    </a>
  </div>
</section>

<section>
  <h2>系统分层架构</h2>
  ${layerStack([
    { label: '客户端层 — Vue3 + Element Plus（用户前台 / 商家后台 / 运营后台）', style: 'client', items: ['Vue 3.4', 'Vite 5.0', 'Element Plus', 'Pinia', 'Axios'] },
    { label: '接入层 — 开发: localhost Nginx / 生产: MetalLB + Nginx Ingress + ModSecurity WAF', style: 'access', items: ['Nginx', 'MetalLB', 'ModSecurity WAF'] },
    { label: '网关层 — Spring Cloud Gateway · JWT鉴权 · Sentinel限流熔断 · HTTP→Dubbo协议转换 · SkyWalking追踪', style: 'gateway', items: ['鉴权认证', '限流熔断', '请求路由', '协议转换', '链路追踪'] },
    { label: '业务服务层 — Dubbo 3.x Triple Protocol · Nacos服务发现 · 18个微服务', style: 'business', items: ['用户域(4)', '商品域(4)', '交易域(2)', '支付', '营销域(2)', '搜索', '商家域', '平台域', '基础支撑(3)'] },
    { label: '中间件层 — RocketMQ · Redis Cluster · Elasticsearch · Seata AT · Sentinel · XXL-Job · SkyWalking · Redisson', style: 'middleware', items: ['RocketMQ 5.1', 'Redis 7.2', 'ES 7.17', 'Seata 1.7', 'Sentinel', 'SkyWalking 9.7'] },
    { label: '数据存储层 — MySQL 8.0 (分库分表) · MongoDB · MinIO · ClickHouse · ShardingSphere-JDBC · Canal数据同步', style: 'storage', items: ['MySQL 8.0', 'MongoDB 7.0', 'MinIO', 'ClickHouse', 'ShardingSphere', 'Canal'] },
    { label: '基础设施层 — Docker Compose (开发) · Kubernetes 1.29 (生产) · GitLab CI/CD · Prometheus + Grafana', style: 'infra', items: ['Docker Compose', 'Kubernetes', 'GitLab CI/CD', 'Prometheus', 'Grafana'] },
  ])}
</section>

<section>
  <h2>实施进度</h2>
  ${timeline([
    { name: 'Phase 1', date: 'W1-W2', title: '基础设施搭建', detail: 'Docker Compose · Maven · Gateway · CI/CD · K8s', status: 'done' },
    { name: 'Phase 2', date: 'W3-W6', title: '用户域 + 商品域', detail: '8个服务 · DDL · 35+测试', status: 'done' },
    { name: 'Phase 3', date: 'W7-W10', title: '交易域 + 支付域', detail: '3个服务 · Seata · RocketMQ · 48测试', status: 'done' },
    { name: 'Phase 4', date: 'W11-W13', title: '搜索 + 营销域', detail: '4个服务 · Canal ES同步 · 优惠券 · 秒杀 Redis Lua · 评价', status: 'done' },
    { name: 'Phase 5', date: 'W14-W16', title: '支撑服务 + 完善', detail: '评价 · 通知 · 文件 · 运营后台 · 监控', status: '' },
    { name: 'Phase 6', date: 'W17-W18', title: '压测 + 优化', detail: 'JMeter · JVM调优 · 高可用演练', status: '' },
  ])}
</section>

<section>
  <h2>微服务全景</h2>
  ${svcMesh([
    { items: [{ label: 'Gateway :8999', style: 'gateway' }] },
    { items: [
      { label: '用户服务 :9301', style: 'user' }, { label: '认证授权 :9302', style: 'user' },
      { label: '会员服务 :9303', style: 'user' }, { label: '地址服务 :9304', style: 'user' },
    ]},
    { items: [
      { label: '商品服务 :9311', style: 'product' }, { label: '类目服务 :9312', style: 'product' },
      { label: '库存服务 :9313', style: 'product' }, { label: '评价服务 :9314', style: 'product' },
    ]},
    { items: [
      { label: '购物车 :9321', style: 'order' }, { label: '订单服务 :9322', style: 'order' },
      { label: '支付服务 :9331', style: 'pay' },
    ]},
    { items: [
      { label: '商家店铺 :9341', style: 'mid' }, { label: '优惠券 :9351', style: 'mid' },
      { label: '秒杀 :9352', style: 'mid' }, { label: '搜索 :9361', style: 'mid' },
    ]},
    { items: [
      { label: '评价 :9314', style: 'mid' }, { label: '文件服务 :9371', style: 'mid' },
      { label: '通知服务 :9372', style: 'mid' }, { label: '运营平台 :9381', style: 'mid' },
    ]},
  ])}
</section>
`;

fs.writeFileSync(path.join(outDir, 'index.html'), page('Super Market — 项目文档', indexBody, 'Index'), 'utf-8');
console.log('Generated: index.html');

console.log('\nDone! Open docs/html/index.html in your browser.');
