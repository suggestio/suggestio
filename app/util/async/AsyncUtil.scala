package util.async

import akka.ConfigurationException
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits
import util.PlayLazyMacroLogsImpl

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 10:54
 * Description: Утиль для управления асинхроном. В частности, тут собственные thread-pool'ы для нужды проекта.
 */
object AsyncUtil extends PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Т.к. контекст может быть не настроен в конфиге, то нужно рендерить сообщение о проблеме в консоль. */
  def fallbackContext(ck: String, ex: Throwable, dfltPar: EcParInfo): ExecutionContext = {
    val msg =
      s"""Failed to create execution context. Please add something like this into application.conf:
        |$ck {
        |  fork-join-executor {
        |    parallelism-factor = ${dfltPar.parFactor}
        |    parallelism-max = ${dfltPar.parMax}
        |  }
        |}
      """.stripMargin
    if (ex.isInstanceOf[ConfigurationException])
      warn(msg)
    else
      warn(msg, ex)
    Implicits.defaultContext
  }

  /** Собрать контекст, откатившись при ошибке на дефолтовый play-context. */
  def mkEc(name: String, dfltPar: => EcParInfo = EcParInfo(10.0F, 2)): ExecutionContext = {
    mkEcOrFallback(name) { ex =>
      fallbackContext(name, ex, dfltPar)
    }
  }

  /** Собрать execution context. При ошибке вызвать функцию fallbackF. */
  def mkEcOrFallback(name: String)(fallbackF: Throwable => ExecutionContext): ExecutionContext = {
    try {
      Akka.system.dispatchers.lookup(name)
    } catch {
      case ex: ConfigurationException =>
        fallbackF(ex)
    }
  }

  /** thread-pool для синхронных запросов к postgres'у. Это позволяет избежать блокировок основного пула. */
  implicit val jdbcExecutionContext = mkEc("async.ec.jdbc")

  /** thread-pool из одного треда для блокирующих операций, обычно есть какая-то внутренняя синхронизация. */
  implicit val singleThreadIoContext = mkEc("async.ec.iosingle", EcParInfo(1.0F, 1))

  /** thread-pool из для внешних cpu-тяжелых операций. */
  implicit val singleThreadCpuContext = mkEc("async.ec.cpusingle", EcParInfo(1.0F, 2))

}

case class EcParInfo(parFactor: Float, parMax: Int)
