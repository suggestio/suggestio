package io.suggest.err

import play.api.mvc.Result

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.2019 9:09
  * Description: Исключение, которое удобно для for-comprehension контура, чтобы прерывать исполнение,
  * возвращая необходимый http-ответ.
  * После for-yield вызывается recoverWith() и матчится экзепшен с http-результатом.
  */

case class HttpResultingException(
                                   httpResFut: Future[Result]
                                 )
  extends RuntimeException
{
  override def toString: String =
    s"${getClass.getSimpleName}(${httpResFut.value.fold("")(_.toString)})"
}


/** Произвольное исключение с абстрактным результатом вычисления внутри. */
case class ResultingException[T](result: T) extends RuntimeException
