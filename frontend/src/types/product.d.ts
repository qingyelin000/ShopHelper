export interface ProductSummary {
  id: string
  categoryId: string
  name: string
  mainImage: string
  price: number
  salesCount: number
}

export interface ProductSku {
  id: string
  skuCode: string
  spec: Record<string, string | number | boolean | null>
  price: number
  stock: number
  status: string
}

export interface ProductDetail {
  id: string
  categoryId: string
  categoryName?: string | null
  name: string
  subTitle?: string | null
  mainImage: string
  description?: string | null
  price: number
  salesCount: number
  status: string
  skuList: ProductSku[]
  createTime: string
  updateTime: string
}

export interface SearchHighlight {
  name?: string
}

export interface SearchProduct {
  id: string
  name: string
  mainImage: string
  price: number
  salesCount: number
  highlight?: SearchHighlight
}

export interface SearchPriceRangeBucket {
  label: string
  count: number
}

export interface SearchCategoryBucket {
  id: string
  name: string
  count: number
}

export interface SearchAggregations {
  priceRanges: SearchPriceRangeBucket[]
  categories: SearchCategoryBucket[]
}

export interface SearchProductPage {
  list: SearchProduct[]
  total: number
  pageNum: number
  pageSize: number
  hasNext: boolean
  aggregations: SearchAggregations
}
