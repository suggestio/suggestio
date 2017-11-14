package io.suggest.sc.router.c

import io.suggest.routes.scRoutes
import io.suggest.sc.sc3.MSc3Resp
import io.suggest.sjs.common.model.Route
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.{Json, OWrites}
import io.suggest.routes.JsRoutes_ScControllers._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:53
  * Description: Под утиль для js-роутера выдачи.
  */
object ScJsRoutesUtil {

  /** Все реквесты выдачи очень похожи: они отличаются только ссылкой для запроса,
    * но и ссылка по факту в генерится похожими путями.
    *
    * @param args Модель аргументов запроса.
    * @param route Функция сборки ссылки на основе сериализованных аргументов запроса.
    * @tparam ArgsT Тип модели аргументов запроса.
    * @return Фьючерс с ответом сервера в стандартном формате.
    */
  def mkSc3Request[ArgsT: OWrites](args: ArgsT, route: js.Dictionary[js.Any] => Route): Future[MSc3Resp] = {
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
