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

trait IHttpRespMapped[T] extends IHttpResultHolder[T] {

  def httpResultHolder: IHttpResultHolder[HttpResp]

  override def abortOrFail(): Unit =
    httpResultHolder.abortOrFail()

  override def mapResult[T2](f: Future[T] => Future[T2]): HttpRespMapped[T2] = {
    HttpRespMapped(
      httpResultHolder = httpResultHolder,
      resultFut = f( resultFut )
    )
  }

}

case class HttpRespMapped[T](
                              httpResultHolder                  : IHttpResultHolder[HttpResp],
                              override val resultFut            : Future[T],
                            )
  extends IHttpRespMapped[T]
