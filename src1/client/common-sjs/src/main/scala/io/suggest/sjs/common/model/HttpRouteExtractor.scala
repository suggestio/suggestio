package io.suggest.sjs.common.model

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

  // TODO def absoluteUrl(secure: Boolean): String
  // TODO def webSocketURL(secure: Boolean = js.native): String

}


/** Задание произвольной роуты вручную. */
case class HttpRoute(
                      method  : String,
                      url     : String
                    )


object HttpRouteExtractor {

  /** Поддержка экстракции роут для инстансов [[Route]]. */
  implicit object PlayJsRouterHre extends HttpRouteExtractor[Route] {
    override def url(t: Route)    = t.url
    override def method(t: Route) = t.method
  }

  /** Поддержка экстракции роут для [[HttpRoute]]. */
  implicit object IHttpRouteHre extends HttpRouteExtractor[HttpRoute] {
    override def url(t: HttpRoute)     = t.url
    override def method(t: HttpRoute)  = t.method
  }

}
