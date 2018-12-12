package io.suggest.sc.u.api

import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{Route, _}
import io.suggest.sc.sc3.MSc3Resp
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
  def REQ_TIMEOUT_MS = Some(20000)


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
    val req = HttpReq.routed(
      route = route(argsJsDict),
      data  = HttpReqData(
        headers   = HttpReqData.headersJsonAccept,
        timeoutMs = REQ_TIMEOUT_MS,
        /*cache = MHttpCacheInfo(
          policy = MHttpCachingPolicies.NetworkFirst,
          // TODO rewriteUrl
        )*/
      )
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[RespT]
  }

}
