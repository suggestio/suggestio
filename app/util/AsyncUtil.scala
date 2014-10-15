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
  private def fallbackContext(ck: String, ex: ConfigurationException): ExecutionContext = {
    warn(
      s"""Failed to create execution context. Please add something like this into application.conf:
        |$ck {
        |  fork-join-executor {
        |    parallelism-factor = 10.0
        |    parallelism-max = 2
        |  }
        |}
      """.stripMargin,
      ex
    )
    Implicits.defaultContext
  }

  /** thread-pool для синхронных запросов к postgres'у. Это позволяет избежать блокировок основного пула. */
  implicit val jdbcExecutionContext: ExecutionContext = {
    val ck = "async.ec.jdbc"
    try {
      Akka.system.dispatchers.lookup(ck)
    } catch {
      case ex: ConfigurationException =>
        fallbackContext(ck, ex)
    }
  }

}
