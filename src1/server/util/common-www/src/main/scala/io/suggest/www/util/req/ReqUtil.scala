package io.suggest.www.util.req

import boopickle.Pickler
import com.google.inject.{Inject, Singleton}
import io.suggest.pick.PickleUtil
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.{BodyParser, BodyParsers, Result, Results}

import scala.concurrent.ExecutionContext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 22:20
  * Description: Утиль для какой-то обобщённой работы с реквестами.
  */
@Singleton
class ReqUtil @Inject() (
                          implicit private val ec: ExecutionContext
                        )
  extends BodyParsers
  with MacroLogsImpl
{

  /** Сборка BodyParser'а, который десериализует тело запроса через boopickle. */
  def picklingBodyParser[T](implicit pickler: Pickler[T]): BodyParser[T] = {
    parse.raw(maxLength = 2048)
      // Десериализовать тело реквеста...
      .validate { rawBuf =>
        rawBuf.asBytes()
          .toRight[Result]( Results.EntityTooLarge("missing body") )
          .right.flatMap { bStr =>
            try {
              val bbuf = bStr.asByteBuffer
              val mfs = PickleUtil.unpickle[T](bbuf)
              Right( mfs )
            } catch { case ex: Throwable =>
              LOGGER.error(s"picklingBodyParser($pickler): unable to deserialize req.body", ex)
              Left( Results.BadRequest("invalid body") )
            }
          }
      }
  }

}
