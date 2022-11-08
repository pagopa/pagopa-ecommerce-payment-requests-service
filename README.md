# pagopa-ecommerce-payment-requests-service

## What is this?

_pagoPA - eCommerce_ microservice to retrieve _payment requests_ data or manage _carts_ (a set of _payment requests_)
with redirects to [_pagoPA – Checkout_](https://checkout.pagopa.it).

### Environment variables

| Variable name           | Description                                                                 | type          | default |
|-------------------------|-----------------------------------------------------------------------------|---------------|---------|
| CHECKOUT_URL            | Redirection URL for Checkout carts                                          | url (string)  |         |
| REDIS_HOST              | Host where the redis instance used to persist idempotency keys can be found | string        |         |
| REDIS_PASSWORD          | Password used for connecting to Redis instance                              | string        |         |
| REDIS_PORT              | Port used for connecting to Redis instance                                  | number        |         |
| REDIS_SSL_ENABLED       | Whether SSL is enabled when connecting to Redis                             | boolean       |         |
| NODO_HOSTNAME           | Nodo connection host name                                                   | string        |         |
| NODO_PER_PSP_URI        | Nodo per PSP URI                                                            | string        |         |
| NODE_FOR_PSP_URI        | Nodo for PSP URI                                                            | string        |         |
| NODO_READ_TIMEOUT       | Http read timeout for all call made to Nodo                                 | number        |         |
| NODO_CONNECTION_TIMEOUT | Http connection timeout for all call made to Nodo                           | number        |         |
| NODO_CONNECTION_STRING  | Connection string containing information used to make Nodo calls            | json (string) |         |

An example configuration of these environment variables is in the `.env.example` file.

## Run the application with `Docker`

Create your environment typing :

```sh
cp .env.example .env
``` 

Then from current project directory run :

```sh
docker-compose up

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
