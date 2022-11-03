# pagopa-ecommerce-payment-requests-service

## What is this?

_pagoPA - eCommerce_ microservice to retrieve _payment requests_ data or manage _carts_ (a set of _payment requests_) with redirects to [_pagoPA – Checkout_](https://checkout.pagopa.it).

### Environment variables

| Variable name     | Description                                                                 | type         | default |
|-------------------|-----------------------------------------------------------------------------|--------------|---------|
| CHECKOUT_URL      | Redirection URL for Checkout carts                                          | url (string) |         |
| REDIS_HOST        | Host where the redis instance used to persist idempotency keys can be found | string       |         |
| REDIS_PASSWORD    | Password used for connecting to Redis instance                              | string       |         |
| REDIS_PORT        | Port used for connecting to Redis instance                                  | number       |         |
| REDIS_SSL_ENABLED | Whether SSL is enabled when connecting to Redis                             | boolean      |         |

An example configuration of these environment variables is in the `.env.example` file.

## Run the application with `springboot-plugin`

Setup your environment typing :
```sh
$ cp .env.example .env
$ export $(grep -v '^#' .env | xargs)
```

Then from the root project directory run :
```sh
$ mvn spring-boot:run
```
