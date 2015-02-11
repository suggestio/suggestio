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

  def futureTimeout: FiniteDuration = 10 seconds

  /** Хелпер для быстрой синхронизации фьючерсов. */
  implicit protected def awaitFuture[T](fut: Awaitable[T]) = {
    try {
      Await.result(fut, futureTimeout)
    } catch {
      case ex: Throwable =>
        val logger = LoggerFactory.getLogger(getClass)
        logger.error("Failed to execute async JMX action: " + fut.toString, ex)
        throw ex
    }
  }

  /** Если на выходе ожидается строка, то можно отрендерить экзепшен вместо re-throw. */
  def awaitString(fut: Awaitable[String]): String = {
    try {
      fut: String
    } catch {
      case ex: Throwable =>
        formatThrowable(ex)
    }
  }

  def formatThrowable(ex: Throwable) = {
    s"${ex.getClass.getSimpleName} occured: ${ex.getMessage}\n\n${ex.getStackTrace.mkString("", "\n", "\n")}"
  }

}
