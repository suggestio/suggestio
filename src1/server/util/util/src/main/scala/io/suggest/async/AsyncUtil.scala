package io.suggest.async

import akka.ConfigurationException
import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsImplLazy

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 10:54
 * Description: Утиль для управления асинхроном. В частности, тут собственные thread-pool'ы для нужды проекта.
 */
@Singleton
final class AsyncUtil @Inject() (
                                  actorSystem : ActorSystem,
                                  defaultEc   : ExecutionContext
                                )
  extends MacroLogsImplLazy
{

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
    import LOGGER.warn
    if (ex.isInstanceOf[ConfigurationException])
      warn(msg)
    else
      warn(msg, ex)
    defaultEc
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
      actorSystem.dispatchers.lookup(name)
    } catch {
      case ex: ConfigurationException =>
        fallbackF(ex)
    }
  }

  /** thread-pool для синхронных запросов к postgres'у. Это позволяет избежать блокировок основного пула. */
  @deprecated("Use slick instead", "2016.sep.5")
  implicit lazy val jdbcExecutionContext = mkEc("async.ec.jdbc")

  /** thread-pool из одного треда для блокирующих операций, обычно есть какая-то внутренняя синхронизация. */
  implicit val singleThreadIoContext = mkEc("async.ec.iosingle", EcParInfo(1.0F, 1))

  /** thread-pool из для внешних cpu-тяжелых операций. */
  implicit val singleThreadCpuContext = mkEc("async.ec.cpusingle", EcParInfo(1.0F, 2))

}

case class EcParInfo(parFactor: Float, parMax: Int)

/** Интерфейс для DI-поля с инстансом [[AsyncUtil]]. */
trait IAsyncUtilDi {
  def asyncUtil: AsyncUtil
}
