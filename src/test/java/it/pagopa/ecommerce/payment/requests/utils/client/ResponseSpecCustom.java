package it.pagopa.ecommerce.payment.requests.utils.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ResponseSpecCustom implements WebClient.ResponseSpec {
    public abstract HttpStatusCode getStatus();

    public WebClient.ResponseSpec onStatus(
                                           Predicate<HttpStatusCode> statusPredicate,
                                           Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction
    ) {
        if (statusPredicate.test(this.getStatus()))
            exceptionFunction.apply(ClientResponse.create(this.getStatus()).build()).block();
        return this;
    }
}
