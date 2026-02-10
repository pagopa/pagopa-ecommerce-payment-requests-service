package it.pagopa.ecommerce.payment.requests.repositories.v2

import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import java.util.*
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.redis.core.RedisHash

@RedisHash("carts", timeToLive = 10 * 60L)
data class CartInfoV2
@PersistenceCreator
constructor(
    @Id val id: UUID,
    val payments: List<PaymentInfo>,
    val idCart: String?,
    val returnUrls: ReturnUrlsV2,
    val email: String?
)

data class ReturnUrlsV2
@PersistenceCreator
constructor(val returnSuccessUrl: String, val returnErrorUrl: String, val returnCancelUrl: String, val returnWaitingUrl: String)
