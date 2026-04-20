/** 商品分类 */
export interface Category {
  id: string
  name: string
  parentId: string
  level: number
  sort: number
}

/** 商品 SKU */
export interface ProductSku {
  id: string
  productId: string
  specJson: string
  price: number
  stock: number
  salesCount: number
}

/** 商品 */
export interface Product {
  id: string
  name: string
  categoryId: string
  categoryName?: string
  mainImage: string
  images: string[]
  description: string
  minPrice: number
  maxPrice: number
  totalStock: number
  totalSales: number
  status: number
  skuList?: ProductSku[]
  createdAt: string
}

/** 搜索结果商品 */
export interface SearchProduct {
  id: string
  name: string
  mainImage: string
  price: number
  salesCount: number
  highlightName?: string
}
