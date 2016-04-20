package io.suggest.sc.sjs.m.mmap

import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.mapbox.gl.map.GlMap

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 12:09
  * Description: Утиль для запроса карты узлов с сервера.
  */
object MNodesSourcesSrv {

  def forMap(glMap: GlMap): Future[MapNodesResp] = {
    val args = MapNodesReqArgs(
      llb   = glMap.getBounds(),
      zoom  = glMap.getZoom()
    )
    val route = routes.controllers.MarketShowcase.renderMapNodesLayer( args.toJson )

    Xhr.getJson( route )
      .asInstanceOf[ Future[MapNodesResp] ]
  }

}
