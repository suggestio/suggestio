package io.suggest.sjs.common.xhr

import io.suggest.sjs.common.model.HttpRouteExtractor
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.18 22:50
  * Description: Модель данных реквеста.
  * Появилась для абстрагирования HTTP-реквестов над разными нижележащими
  * http-клиентами (fetch(), XmlHTTPRequest, etc),
  * а также для отказа от передачи пачек аргументов между разными методами внутри [[Xhr]].
  */
object HttpReq {

  @inline implicit def univEq: UnivEq[HttpReq] = UnivEq.derive


  /** Сборка реквеста на базе привычной роуты. */
  def routed[HttpRoute: HttpRouteExtractor](route     : HttpRoute,
                                            data      : HttpReqData = HttpReqData.empty,
                                           ): HttpReq = {
    val hre = implicitly[HttpRouteExtractor[HttpRoute]]
    apply(
      method    = hre.method(route),
      url       = Xhr.route2url(route),
      data      = data,
    )
  }

}


/** Модель описания реквеста HTTP.
  *
  * @param method Метод.
  * @param url Ссылка.
  * @param data Необязательные данные реквеста.
  *             Унесены в отдельную модель, чтобы не пробрасывать кучу аргументов из routed() и прочим причинам.
  */
case class HttpReq(
                    method     : String,
                    url        : String,
                    data       : HttpReqData = HttpReqData.empty,
                  ) {

  def withDetails(details: HttpReqData) = copy(data = details)

}
