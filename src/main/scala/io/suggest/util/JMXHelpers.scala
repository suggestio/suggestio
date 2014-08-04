package io.suggest.util

import javax.management.ObjectName
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 10:35
 * Description: Разные хелперы и утиль для работы с JMX.
 */
object JMXHelpers {

  implicit def string2objectName(name:String):ObjectName = new ObjectName(name)

}

// Базовые костыли для всех реализаций jmx-адаптеров.
trait JMXBase {

  def jmxName: String

  /** Хелпер для быстрой синхронизации фьючерсов. */
  implicit protected def awaitFuture[T](fut: Awaitable[T]) = {
    try {
      Await.result(fut, 10 seconds)
    } catch {
      case ex: Throwable =>
        val logger = LoggerFactory.getLogger(getClass)
        logger.error("Failed to execute async JMX action: " + fut.toString, ex)
        // TODO Нужно возвращать сериализабельный экзепшен, чтобы юзеру нормально отобразился.
        throw ex
    }
  }

}
