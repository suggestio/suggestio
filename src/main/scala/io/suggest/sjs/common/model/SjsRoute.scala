package io.suggest.sjs.common.model

import io.suggest.sjs.common.xhr.Xhr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 12:24
  * Description: Модель данных по роуте, которая используется вместо Route, когда это уместно.
  */

object SjsRoute {

  def fromJsRoute(r: Route): SjsRoute = {
    SjsRoute(
      method  = r.method,
      url     = r.url
    )
  }

  def fromJsRouteUsingXhrOpts(r: Route): SjsRoute = {
    SjsRoute(
      method  = r.method,
      url     = Xhr.route2url(r)
    )
  }

}

case class SjsRoute(method: String, url: String)
