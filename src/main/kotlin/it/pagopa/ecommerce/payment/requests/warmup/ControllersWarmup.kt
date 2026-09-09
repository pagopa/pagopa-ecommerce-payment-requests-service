package it.pagopa.ecommerce.payment.requests.warmup

import it.pagopa.ecommerce.payment.requests.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.payment.requests.warmup.annotations.WarmupFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.system.measureTimeMillis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.RestController

@Component
class ControllersWarmup : ApplicationListener<ContextRefreshedEvent> {

  private val logger = LoggerFactory.getLogger(this.javaClass)
  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    val restControllers =
      event.applicationContext.getBeansWithAnnotation<RestController>().map { it.value }
    if (logger.isDebugEnabled) {
      LogTracingUtils.loggerTracingUtils()
        .success()
        .details(mapOf("controllers_count" to restControllers.size.toString()))
        .logDebug(logger, "Controllers warm-up founded")
    }
    restControllers.forEach(this::warmUpController)
  }

  private fun warmUpController(controllerToWarmUpInstance: Any) {
    var warmUpMethods = 0
    val controllerToWarmUpKClass = ClassUtils.getUserClass(controllerToWarmUpInstance).kotlin
    val elapsedTime = measureTimeMillis {
      runCatching {
          controllerToWarmUpKClass.declaredMemberFunctions
            .filter { it.hasAnnotation<WarmupFunction>() }
            .forEach { method ->
              warmUpMethods++
              val result: Result<*>
              val intertime = measureTimeMillis {
                result = runCatching { method.call(controllerToWarmUpInstance) }
              }
              result
                .onSuccess { _ ->
                  LogTracingUtils.loggerTracingUtils()
                    .success()
                    .details(
                      mapOf(
                        "warmup_methods_count" to method.toString(),
                        "elapsed_time" to intertime.toString()))
                    .logInfo(logger, "Controllers warm-up completed")
                }
                .getOrElse { exception ->
                  LogTracingUtils.loggerTracingUtils()
                    .failure()
                    .details(
                      mapOf(
                        "warmup_methods" to method.toString(),
                        "elapsed_time" to intertime.toString()))
                    .logError(logger, exception, "Controllers warm-up execution error!")
                }
            }
        }
        .getOrElse {
          LogTracingUtils.loggerTracingUtils()
            .failure()
            .logErrorWithStackTrace(logger, it, "Exception performing controllers warm-up")
        }
    }
    LogTracingUtils.loggerTracingUtils()
      .success()
      .details(
        mapOf(
          "controller" to controllerToWarmUpKClass.toString(),
          "warmup_methods_count" to warmUpMethods.toString(),
          "elapsed_time_ms" to elapsedTime.toString()))
      .logInfo(logger, "Controllers warm-up completed")
  }
}
