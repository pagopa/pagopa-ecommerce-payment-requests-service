package it.pagopa.ecommerce.payment.requests.warmup.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
/**
 * Annotation used to annotate a controller function to be called during module warm-up phase.
 * Warm-up function can be used to send request to a RestController in order to initialize all it's resource
 * before the module being ready to serve requests
 */
annotation class WarmupFunction {
}