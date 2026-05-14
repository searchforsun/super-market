import request from '../request'

export function getActiveBanners(position = 'HOME_TOP') {
  return request.get('/platform/banners', { params: { position } })
}
