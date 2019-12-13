package io.suggest.proto.http.model

import japgolly.univeq.UnivEq

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.12.2019 13:28
  * Description: Контейнер для связки http-ответа с отображённым результатом.
  * Отражение происходит через эту внешнюю модель-контейнер.
  */
object HttpRespMapped {
  @inline implicit def univEq[T: UnivEq]: UnivEq[HttpRespMapped[T]] = UnivEq.force
}

case class HttpRespMapped[T](
                              httpRespHolder       : HttpRespHolder,
                              resultFut            : Future[T],
                            )
  extends IMapResult[T]
{

  override def mapResult[T2](f: Future[T] => Future[T2]): HttpRespMapped[T2] = {
    copy(
      resultFut = f(resultFut),
    )
  }

}


/** Общий интерфейс для отражения результата http-запроса в новое состояние. */
trait IMapResult[T] {
  def mapResult[T2](f: Future[T] => Future[T2]): HttpRespMapped[T2]
}