package br.com.quintoandar.tax.core.order

import br.com.quintoandar.tax.core.ItemSlug
import java.time.LocalDate

data class Order(
    val id: OrderId,
    val date: LocalDate,
    val items: List<Item>,
) {
    data class Item(
        val slug: ItemSlug,
        val value: Double,
        val actorId: ActorId,
    )
}

typealias OrderId = String
