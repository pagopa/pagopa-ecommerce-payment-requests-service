package it.pagopa.ecommerce.payment.requests.repositories

import java.util.*
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.redis.core.RedisHash

@RedisHash("carts", timeToLive = 10 * 60L)
data class CartInfo
@PersistenceCreator
constructor(
  @Id val cartId: UUID,
  val payments: List<PaymentInfo>,
  val returnUrls: ReturnUrls,
  val email: String?
)

data class ReturnUrls
@PersistenceCreator
constructor(val returnSuccessUrl: String, val returnErrorUrl: String, val returnCancelUrl: String)
