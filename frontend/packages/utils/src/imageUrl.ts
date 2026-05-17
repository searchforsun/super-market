/**
 * Convert legacy absolute MinIO URLs to gateway-relative paths.
 * e.g. http://ecs4c16g:9000/smt-product/xxx.jpeg → /api/file/raw/smt-product/xxx.jpeg
 */
export function resolveImageUrl(url: string | undefined | null): string {
  if (!url) return ''
  // Already a relative gateway path — use as-is
  if (url.startsWith('/api/file/raw/')) return url
  // Legacy absolute MinIO URL — extract bucket/key and rewrite
  const m = url.match(/\/\/([^/]+)\/(smt-[^/]+)\/(.+)/)
  if (m) return `/api/file/raw/${m[2]}/${m[3]}`
  // Other absolute URLs — assume accessible
  return url
}
