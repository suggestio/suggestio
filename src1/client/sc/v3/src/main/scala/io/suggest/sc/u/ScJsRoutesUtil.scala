package io.suggest.sc.u

import io.suggest.sc.sc3.MSc3Resp
import io.suggest.sc.{Sc3Api, ScConstants}
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.{Json, OWrites, Reads}

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:53
  * Description: Под утиль для js-роутера выдачи.
  */
object ScJsRoutesUtil {

  /** Через сколько секунд запрос можно закрывать по таймауту? */
  def REQ_TIMEOUT_MS = Some(10000)


  /** Все реквесты выдачи очень похожи: они отличаются только ссылкой для запроса,
    * но и ссылка по факту в генерится похожими путями.
    *
    * @param args Модель аргументов запроса.
    * @param route Функция сборки ссылки на основе сериализованных аргументов запроса.
    * @tparam ArgsT Тип модели аргументов запроса.
    * @return Фьючерс с ответом сервера в стандартном формате.
    */
  def mkSc3Request[ArgsT: OWrites](args: ArgsT, route: js.Dictionary[js.Any] => Route): Future[MSc3Resp] = {
    mkRequest[ArgsT, MSc3Resp](args, route)
  }

  def mkRequest[ArgsT: OWrites, RespT: Reads](args: ArgsT, route: js.Dictionary[js.Any] => Route): Future[RespT] = {
    val argsPj = Json.toJsObject( args )
    val argsJsDict = PlayJsonSjsUtil.toNativeJsonObj( argsPj )

    val vsnKey = ScConstants.ReqArgs.VSN_FN
    if (!argsJsDict.contains(vsnKey))
      argsJsDict.update( vsnKey, Sc3Api.API_VSN.value )

    Xhr.unJsonResp[RespT] {
      Xhr.requestJsonText( route(argsJsDict), REQ_TIMEOUT_MS )
    }
  }

}
