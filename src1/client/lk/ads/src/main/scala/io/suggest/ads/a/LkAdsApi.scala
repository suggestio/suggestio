package io.suggest.ads.a

import io.suggest.ads.MLkAdsOneAdResp
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.routes

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:56
  * Description: API для доступа к серверу за карточками.
  */
trait ILkAdsApi {

  /** Запрос к серверу за карточками.
    *
    * @param nodeKey Ключ текущего узла.
    * @param offset Сдвиг в результатах.
    * @return Фьючерс с порцией карточек.
    */
  def getAds(nodeKey: RcvrKey, offset: Int): Future[Seq[MLkAdsOneAdResp]]

}


/** Реализация [[ILkAdsApi]] поверх традиционных http-запросов. */
class LkAdsApiHttp() extends ILkAdsApi {

  override def getAds(nodeKey: RcvrKey, offset: Int): Future[Seq[MLkAdsOneAdResp]] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAds.getAds(
        rcvrKey = RcvrKey.rcvrKey2urlPath( nodeKey ),
        offset  = offset
      ),
      data = HttpReqData.justAcceptJson
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[Seq[MLkAdsOneAdResp]]
  }

}
