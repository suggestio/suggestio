package io.suggest.sc.u.api

import io.suggest.cordova.CordovaConstants
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.i18n.MLanguage
import io.suggest.msg.JsonPlayMessages
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpClientConfig, HttpReq, HttpReqData}
import io.suggest.routes.routes
import io.suggest.sc.index.{MSc3IndexResp, MScIndexes}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.2020 18:06
  * Description: API для доступа к ScStuff-контроллеру.
  */
trait IScStuffApi {

  /** Заполнение корректными данными указанного списка узлов.
    *
    * @param currNodes Перечисление текущих узлов.
    * @return Фьючерс со списком узлов из ответа.
    */
  def fillNodesList( currNodes: MScIndexes ): Future[List[MSc3IndexResp]]

  /** Read JSON messages from server.
    *
    * @param language language code.
    *                 None asks server to return session-current language.
    * @return Ready to use JSON Play messages.
    */
  def scMessagesJson( language: Option[MLanguage] ): Future[JsonPlayMessages]

}


final class ScStuffApiHttp(
                            httpClientConfig: () => HttpClientConfig,
                          )
  extends IScStuffApi
{

  override def fillNodesList(currNodes: MScIndexes): Future[List[MSc3IndexResp]] = {
    // TODO indexes2 - надо почистить, оставив только id узлов, координаты и может что-то ещё.
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.sc.ScStuff.fillNodesList(),
          data = HttpReqData(
            headers = HttpReqData.headersJsonSendAccept,
            body = Json
              .toJson( currNodes )
              .toString(),
            timeoutMs = Some( 5000 ),
            config = httpClientConfig(),
          )
        )
      )
      .resultFut
      .unJson[List[MSc3IndexResp]]
  }


  override def scMessagesJson(language: Option[MLanguage]): Future[JsonPlayMessages] = {
    val (method, url, httpReqData) = (for {
      lang <- language
      if CordovaConstants.isCordovaPlatform()
    } yield {
      // For cordova with known language, use locally-stored JSON files:
      val fsUrl = "lang/" + lang.value + ".json"
      val reqData = HttpReqData(
        // Fetch API and cordovaFetch - both doesn't support file:/// URLs. Forcing XHR (or need to install and use cordova file plugin)
        forceXhr = true,
      )
      (HttpConst.Methods.GET, fsUrl, reqData)
    })
      .getOrElse {
        val route = routes.controllers.sc.ScStuff.scMessagesJson(
          langCode = language.fold[String](null)(_.value),
        )
        val reqData = HttpReqData(
          headers = HttpReqData.headersJsonAccept,
          config  = httpClientConfig(),
        )
        (route.method, HttpClient.route2url(route), reqData)
      }

    HttpClient
      .execute(
        HttpReq(
          method  = method,
          url     = url,
          data    = httpReqData,
        )
      )
      .resultFut
      .nativeJsonFut[js.Dictionary[String]]
      .map( new JsonPlayMessages(_) )
  }

}
