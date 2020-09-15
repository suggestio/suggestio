package io.suggest.proto.http.model

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.2020 15:24
  * Description: Внутренняя модель между HttpClient и конкретными Http-адаптерами.
  */


/** Контейнер пошаренных данных поверх исходного реквеста.
  * Собирается общим кодом HttpClient'а над адаптерами, и передаётся внутрь адаптеров.
  *
  * @param origReq Оригинальный реквест.
  * @param reqUrlOpt Фактический URL запроса, вместо оригинального.
  * @param allReqHeaders Все заголовки запроса.
  */
final case class HttpReqAdp(
                             origReq           : HttpReq,
                             reqUrlOpt         : Option[String],
                             allReqHeaders     : Map[String, String],
                           ) {

  /** Фактический URL запроса, или оригинал. */
  def reqUrl = reqUrlOpt getOrElse origReq.url

}

