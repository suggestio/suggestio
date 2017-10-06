package io.suggest.util

import javax.management.ObjectName

import io.suggest.di.IExecutionContext
import io.suggest.util.logs.IMacroLogs

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 10:35
 * Description: Разные хелперы и утиль для работы с JMX.
 */
object JMXHelpers {

  def string2objectName(name: String): ObjectName = new ObjectName(name)

}

// Базовые костыли для всех реализаций jmx-адаптеров.
trait JMXBase extends IMacroLogs with IExecutionContext {

  def jmxName: String

  def futureTimeout: FiniteDuration = 60.seconds

  /** Хелпер для быстрой синхронизации фьючерсов. */
  implicit protected def awaitFuture[T](fut: Future[T]): T = {
    // Есть ненулевой риск не дождаться результата. На этот случай, надо вписать результат в логи:
    fut.onComplete {
      case Success(res) => LOGGER.info(s"JMX ok: $res")
      case Failure(ex)  => LOGGER.error(s"JXM fail", ex)
    }
    // Синхронное дожидание результата.
    try {
      Await.result(fut, futureTimeout)
    } catch {
      case ex: Throwable =>
        LOGGER.error("Failed to execute async JMX action: " + fut.toString, ex)
        throw ex
    }
  }

  /** Если на выходе ожидается строка, то можно отрендерить экзепшен вместо re-throw. */
  def awaitString(fut: Future[String]): String = {
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
