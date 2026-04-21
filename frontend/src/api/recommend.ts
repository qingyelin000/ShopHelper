import request from './request'
import type { RecommendationResponse, RecommendationScene } from '@/types/recommend.d'

/** 获取当前用户推荐 */
export function getMyRecommendations(scene: RecommendationScene, pageSize = 10) {
  return request.get<any, RecommendationResponse>('/v1/recommendations/me', {
    params: { scene, pageSize },
  })
}
