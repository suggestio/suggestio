package io.suggest.sjs.common.model

import io.suggest.proto.HttpConst

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 21:55
  * Description: Type-class для извлечения основных данных http-запроса: URL и Method.
  */
sealed trait HttpRouteExtractor[T] {

  /** Прочитать ссылку. */
  def url(t: T): String

  /** Прочитать http-метод. */
  def method(t: T): String

  /** Сборка абсолютной ссылки. */
  def absoluteUrl(t: T, secure: Boolean): String

  /** Сборка абсолютной ссылки для websocket. */
  def webSocketUrl(t: T, secure: Boolean): String

}


/** Задание произвольной роуты вручную.
  *
  * @param method HTTP-метод. Строка вида "GET".
  * @param url URL вида "//host/path..."
  */
case class HttpRoute(
                      method  : String,
                      url     : String
                    ) {

  assert( url.startsWith( HttpConst.Proto.CURR_PROTO ) )

  private def _absUrl(proto: String, secure: Boolean): String = {
    proto +
      (if(secure) HttpConst.Proto.SECURE_SUFFIX else "") +
      HttpConst.Proto.COLON +
      url
  }

  def absoluteUrl(secure: Boolean): String = {
    _absUrl( HttpConst.Proto.HTTP, secure)
  }

  def webSocketUrl(secure: Boolean): String = {
    _absUrl( HttpConst.Proto.WS, secure)
  }

}


object HttpRouteExtractor {

  /** Поддержка экстракции роут для инстансов [[Route]]. */
  implicit object PlayJsRouterHre extends HttpRouteExtractor[Route] {
    override def url(t: Route)    = t.url
    override def method(t: Route) = t.method

    override def absoluteUrl(t: Route, secure: Boolean): String =
      t.absoluteURL(secure)

    override def webSocketUrl(t: Route, secure: Boolean): String =
      t.webSocketURL(secure)
  }

  /** Поддержка экстракции роут для [[HttpRoute]]. */
  implicit object IHttpRouteHre extends HttpRouteExtractor[HttpRoute] {
    override def url(t: HttpRoute)     = t.url
    override def method(t: HttpRoute)  = t.method

    override def absoluteUrl(t: HttpRoute, secure: Boolean): String =
      t.absoluteUrl(secure)

    override def webSocketUrl(t: HttpRoute, secure: Boolean): String =
      t.webSocketUrl(secure)
  }

}
