package io.suggest.ws.pool

import diode.ModelRO
import io.suggest.proto.HttpConst
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.Xhr
import org.scalajs.dom.raw.WebSocket

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 15:23
  * Description: Client-API для WS-channel, т.е. для унифицированного вебсокета.
  */

trait IWsChannelApi {

  /** Открыть WS-channel до указанного хоста.
    *
    * @param host Сервер, на который долбимся с запросом.
    * @return Фьючерс с веб-сокетом.
    */
  def wsChannel(host: String): Future[WebSocket]

}


/** Реализация [[IWsChannelApi]] через HTTP. */
class WsChannelApiHttp(ctxIdRO: ModelRO[String]) extends IWsChannelApi {

  override def wsChannel(host: String): Future[WebSocket] = {
    val route = routes.controllers.Static.wsChannel( ctxIdRO.value )

    val wsProto = HttpConst.Proto.wsOrWss( Xhr.PREFER_SECURE_URLS )
    val wsUrl = wsProto + HttpConst.Proto.DELIM + host + route.url

    val ws = new WebSocket(wsUrl)
    // TODO Стоит наверное задействовать open event. Но в MDN написано, что это experimental feature.

    Future.successful(ws)
  }

}
