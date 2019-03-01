package io.suggest.util

import io.suggest.util.logs.MacroLogsImpl
import javax.management.ObjectName

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 10:35
 * Description: Разные хелперы и утиль для работы с JMX.
 */
object JmxBase extends MacroLogsImpl {

  /** Используем не-play-евский тред-пул для ожидания, чтобы не блокировать основной пул play. */
  import scala.concurrent.ExecutionContext.Implicits.global

  /** Импорт может затянуться, несмотря на все ускорения. Увеличиваем таймаут до получения результата. */
  def AWAIT_TIMEOUT: FiniteDuration = 5.minutes

  def string2objectName(name: String): ObjectName = new ObjectName(name)

  def formatThrowable(ex: Throwable) = {
    s"${ex.getClass.getSimpleName} occured: ${ex.getMessage}\n\n${ex.getStackTrace.mkString("", "\n", "\n")}"
  }

  def tryCatch(f: () => String): String = {
    Try(f())
      .recover { case ex: Throwable =>
        formatThrowable(ex)
      }
      .get
  }

  /** Хелпер для быстрой синхронизации фьючерсов. */
  def awaitFuture[T](fut: Future[T]): T = {
    // Есть ненулевой риск не дождаться результата. На этот случай, надо вписать результат в логи:
    fut.onComplete {
      case Success(res) => LOGGER.debug(s"JMX ok: $res")
      case Failure(ex)  => LOGGER.error(s"JXM fail", ex)
    }
    // Синхронное дожидание результата.
    try {
      Await.result(fut, AWAIT_TIMEOUT)
    } catch {
      case ex: Throwable =>
        LOGGER.error("Failed to execute async JMX action: " + fut.toString, ex)
        throw ex
    }
  }

  /** Если на выходе ожидается строка, то можно отрендерить экзепшен вместо re-throw. */
  def awaitString(fut: Future[String]): String = {
    try {
      awaitFuture[String](fut)
    } catch {
      case ex: Throwable =>
        formatThrowable(ex)
    }
  }

  object Types {
    def UTIL = "util"
    def ELASTICSEARCH = "elasticsearch"
    def COMPAT = "compat"
    def BILL = "bill"
    def STAT = "stat"
    def IPGEOBASE = "ipgeobase"
    def IMG = "img"
  }

}

trait IJmxBase {
  def jmxName: String
}

// Базовые костыли для всех реализаций jmx-адаптеров.
trait JmxBase extends IJmxBase {

  // TODO Переименовать из jmxName в нечто иное. name - это одно из полей внутри этой строки.
  override final def jmxName: String =
    s"io.suggest:type=${_jmxType},name=${getClass.getSimpleName.replace("Jmx", "")}"

  def _jmxType: String

}
