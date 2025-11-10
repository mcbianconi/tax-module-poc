package br.com.quintoandar.tax.containers.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["br.com.quintoandar.tax"],
)
class TaxApi

fun main(args: Array<String>) {
    runApplication<TaxApi>(*args)
}
