package it.pagopa.ecommerce.payment.requests.utils.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Predicate;

public abstract class ResponseSpecCustom implements WebClient.ResponseSpec {

    public abstract HttpStatus getStatus(); // Restituisce HttpStatus, compatibile con HttpStatusCode

    @Override
    public WebClient.ResponseSpec onStatus(
                                           Predicate<HttpStatusCode> statusPredicate,
                                           Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction
    ) {
        HttpStatusCode status = getStatus();
        if (statusPredicate.test(status)) {
            exceptionFunction.apply(ClientResponse.create(status).build()).block();
        }
        return this;
    }
}
