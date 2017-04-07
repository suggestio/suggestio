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
        def logPrefix = s"picklingBodyParser($pickler):"
        rawBuf.asBytes()
          .filter( _.nonEmpty )
          .toRight[Result] {
            LOGGER.error(s"$logPrefix Request body is null or empty.")
            Results.UnprocessableEntity("Request body expected.")
          }
          .right.flatMap { byteStr =>
            try {
              val bbuf = byteStr.asByteBuffer
              val mfs = PickleUtil.unpickle[T](bbuf)
              Right(mfs)
            } catch {
              case ex: Throwable =>
                LOGGER.error(s"$logPrefix Unable to deserialize req.body", ex)
                Left(Results.BadRequest("invalid body"))
            }
          }
      }
  }

}
