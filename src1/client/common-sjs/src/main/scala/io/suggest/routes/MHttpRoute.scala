package io.suggest.routes

import io.suggest.proto.http.HttpConst
import io.suggest.text.UrlUtil2
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.2020 16:11
  * Description: Http-роута, собираемая вручную.
  */
object MHttpRoute {

  @inline implicit def univEq: UnivEq[MHttpRoute] = UnivEq.derive

  /** Ad-hoc polymorphism: typeclass-адаптер для поддержки http-клиента. */
  implicit class MHttpRouteOpsExt( val httpRoute: MHttpRoute ) extends AnyVal {

    def mkAbsUrl(proto: String, secure: Boolean): String = {
      UrlUtil2.mkAbsUrl(
        protoPrefix = proto,
        secure      = secure,
        relUrl      = httpRoute.url,
      )
    }

  }


  /** Поддержка экстракции роут для [[MHttpRoute]]. */
  implicit object HttpRouteHre extends HttpRouteExtractor[MHttpRoute] {
    override def url(t: MHttpRoute)     = t.url
    override def method(t: MHttpRoute)  = t.method

    override def absoluteUrl(t: MHttpRoute, secure: Boolean): String =
      t.mkAbsUrl( HttpConst.Proto.HTTP, secure )

    override def webSocketUrl(t: MHttpRoute, secure: Boolean): String =
      t.mkAbsUrl( HttpConst.Proto.WS, secure )
  }

}


/** Задание произвольной http-роуты вручную.
  *
  * @param method HTTP-метод. Строка вида "GET".
  * @param url URL вида "//host/path..."
  */
case class MHttpRoute(
                       method  : String,
                       url     : String,
                     )
