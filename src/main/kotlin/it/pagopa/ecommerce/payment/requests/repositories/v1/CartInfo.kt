package it.pagopa.ecommerce.payment.requests.repositories.v1

import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import java.util.*
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.redis.core.RedisHash

@RedisHash("carts", timeToLive = 10 * 60L)
data class CartInfo
@PersistenceCreator
constructor(
  @Id val id: UUID,
  val payments: List<PaymentInfo>,
  val idCart: String?,
  val returnUrls: ReturnUrls,
  val email: String?
)

data class ReturnUrls
@PersistenceCreator
constructor(val returnSuccessUrl: String, val returnErrorUrl: String, val returnCancelUrl: String)
