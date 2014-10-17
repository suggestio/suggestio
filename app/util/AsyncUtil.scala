package util

import akka.ConfigurationException
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits
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
    warn(
      s"""Failed to create execution context. Please add something like this into application.conf:
        |$ck {
        |  fork-join-executor {
        |    parallelism-factor = ${dfltPar.parFactor}
        |    parallelism-max = ${dfltPar.parMax}
        |  }
        |}
      """.stripMargin,
      ex
    )
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

}

case class EcParInfo(parFactor: Float, parMax: Int)
