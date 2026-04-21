export type RecommendationScene = 'homepage' | 'product_detail' | 'cart' | 'checkout' | 'category'

export interface RecommendationItem {
  productId: string
  name: string
  mainImage: string
  price: number
  salesCount: number
  recommendTag: string
  score: number
}

export interface RecommendationResponse {
  scene: RecommendationScene
  list: RecommendationItem[]
  total: number
  algorithm: string
}
