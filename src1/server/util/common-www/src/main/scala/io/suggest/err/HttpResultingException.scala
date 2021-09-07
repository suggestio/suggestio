package io.suggest.err

import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.2019 9:09
  * Description: Исключение, которое удобно для for-comprehension контура, чтобы прерывать исполнение,
  * возвращая необходимый http-ответ.
  * После for-yield вызывается recoverWith() и матчится экзепшен с http-результатом.
  */
object HttpResultingException {

  implicit final class HrxOpsExt( private val resFut: Future[Result] ) extends AnyVal {

    /** Перехват [[HttpResultingException]] исключения. */
    def recoverHttpResEx(implicit ec: ExecutionContext): Future[Result] = {
      resFut.recoverWith {
        case ex: HttpResultingException =>
          ex.httpResFut
      }
    }

  }

}


case class HttpResultingException(
                                   httpResFut: Future[Result]
                                 )
  extends RuntimeException
  with NoStackTrace
{

  override final def toString: String =
    s"${getClass.getSimpleName}(${httpResFut.value.fold("")(_.toString)})"

}
