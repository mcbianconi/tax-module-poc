package br.com.quintoandar.tax.core.tax

data class ItemTax(
    val taxType: TaxType,
    val amount: Double,
)
