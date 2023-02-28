package it.pagopa.ecommerce.payment.requests

import it.pagopa.ecommerce.commons.ConfigScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@SpringBootApplication
@EnableRedisRepositories(
  basePackages =
    [
      "it.pagopa.ecommerce.commons.repositories",
      "it.pagopa.ecommerce.payment.requests.repositories"])
@Import(ConfigScan::class)
class PagopaEcommercePaymentRequestsServiceApplication

fun main(args: Array<String>) {
  runApplication<PagopaEcommercePaymentRequestsServiceApplication>(*args)
}
