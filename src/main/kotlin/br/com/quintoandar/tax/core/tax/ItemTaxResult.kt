package br.com.quintoandar.tax.core.tax

import br.com.quintoandar.tax.core.order.Order

data class ItemTaxResult(
    val item: Order.Item,
    val taxes: List<ItemTax>,
)
