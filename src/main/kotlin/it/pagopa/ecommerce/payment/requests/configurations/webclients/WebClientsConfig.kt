package it.pagopa.ecommerce.payment.requests.configurations.webclients

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.ecommerce.payment.requests.utils.soap.Jaxb2SoapDecoder
import it.pagopa.ecommerce.payment.requests.utils.soap.Jaxb2SoapEncoder
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientsConfig {

  @Bean
  fun nodoWebClient(
    @Value("\${nodo.hostname}") nodoHostname: String,
    @Value("\${nodo.readTimeout}") nodoReadTimeout: Int,
    @Value("\${nodo.connectionTimeout}") nodoConnectionTimeout: Int
  ): WebClient {
    val httpClient =
      HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nodoConnectionTimeout)
        .doOnConnected { connection: Connection ->
          connection.addHandlerLast(
            ReadTimeoutHandler(nodoReadTimeout.toLong(), TimeUnit.MILLISECONDS))
        }
        .resolver { it.ndots(1) }
    val exchangeStrategies =
      ExchangeStrategies.builder()
        .codecs { clientCodecConfigurer: ClientCodecConfigurer ->
          val mapper = ObjectMapper()
          mapper.registerModule(JavaTimeModule())
          mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          clientCodecConfigurer.registerDefaults(false)
          clientCodecConfigurer.customCodecs().register(Jaxb2SoapDecoder())
          clientCodecConfigurer.customCodecs().register(Jaxb2SoapEncoder())
          clientCodecConfigurer
            .customCodecs()
            .register(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
          clientCodecConfigurer
            .customCodecs()
            .register(Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON))
        }
        .build()
    return WebClient.builder()
      .baseUrl(nodoHostname)
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .exchangeStrategies(exchangeStrategies)
      .build()
  }

  @Bean
  fun objectFactoryNodeForPsp(): it.pagopa.ecommerce.generated.transactions.model.ObjectFactory =
    it.pagopa.ecommerce.generated.transactions.model.ObjectFactory()
}
