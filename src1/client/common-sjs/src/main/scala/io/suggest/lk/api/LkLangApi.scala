package io.suggest.lk.api

import io.suggest.i18n.{I18nConst, MLanguage}
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpClientConfig, HttpReq, HttpReqData}
import io.suggest.routes.routes
import play.api.libs.json.Json

import scala.concurrent.Future

trait ILkLangApi {

  /** Save language on server.
    *
    * @param lang New language choosen.
    * @return Future with no result.
    */
  def selectLangSubmit(lang: MLanguage): Future[_]

}


class LkLangApiHttpImpl(
                         httpClientConfig: () => HttpClientConfig,
                       )
  extends ILkLangApi
{

  /** Save language on server.
    *
    * @param lang New language choosen.
    * @return Future with no result.
    */
  override def selectLangSubmit(lang: MLanguage): Future[_] = {
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.LkLang.selectLangSubmit( async = true ),
          data = HttpReqData(
            headers = HttpReqData.headersJsonSend,
            body = Json
              .obj(
                I18nConst.LANG_SUBMIT_FN -> lang.value,
              )
              .toString(),
            config = httpClientConfig(),
          )
        )
      )
      .resultFut
      .successIf200
  }

}
