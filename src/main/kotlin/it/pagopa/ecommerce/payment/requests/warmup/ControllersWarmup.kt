package it.pagopa.ecommerce.payment.requests.warmup

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
    logger.info("Found controllers: [{}]", restControllers.size)
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
                result = runCatching {
                  logger.info("Invoking function: [{}]", method.toString())
                  method.call(controllerToWarmUpInstance)
                }
              }
              result
                .onSuccess { _ ->
                  logger.info(
                    "Warmup function: [{}] -> elapsed time: [{}].", method.toString(), intertime)
                }
                .getOrElse { exception ->
                  logger.error(
                    "Warmup function: [$method] execution error! Elapsed time: [${intertime}].",
                    exception)
                }
            }
        }
        .getOrElse { logger.error("Exception performing controller warm up ", it) }
    }
    logger.info(
      "Controller: [{}] warm-up executed functions: [{}], elapsed time: [{}] ms",
      controllerToWarmUpKClass,
      warmUpMethods,
      elapsedTime)
  }
}
