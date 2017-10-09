package io.suggest.sc.inx.c

import io.suggest.routes.scRoutes
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.resp.MSc3Resp
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.xplay.json.PlayJsonSjsUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json
import io.suggest.routes.JsRoutes_ScControllers._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:54
  * Description: Интерфейс API для получения индекса с сервера.
  */
trait IIndexApi {

  /** Получить индекс с сервера, вернув MSc3Resp.
    *
    * @param args Аргументы запроса индекса.
    * @return Фьючерс с ответом сервера.
    */
  def getIndex(args: MScIndexArgs): Future[MSc3Resp]

}


/** Реализация [[IIndexApi]] поверх XHR. */
trait IndexApiXhrImpl extends IIndexApi {

  override def getIndex(args: MScIndexArgs): Future[MSc3Resp] = {
    val argsPj = Json.toJsObject( args )
    val argsJsDict = PlayJsonSjsUtil.toNativeJsonObj( argsPj )
    val route = scRoutes.controllers.Sc.index( argsJsDict )
    for {
      respJsonText <- Xhr.requestJsonText( route )
    } yield {
      Json
        .parse( respJsonText )
        .as[MSc3Resp]
    }
  }

}
