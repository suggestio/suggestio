package io.suggest.maps.u

import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.client.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.model._
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.17 16:27
  * Description: js-роуты для Static-контроллера.
  */

/** Интерфейс для статического API. */
trait IAdvRcvrsMapApi {

  /** Получение десериализованного инстанса с данными гео.карты рекламщиков. */
  def advRcvrsMapJson(args: MRcvrsMapUrlArgs): Future[MGeoNodesResp]

}

object AdvRcvrsMapApiHttpViaUrl {

  def httpClientConfigDefault() = {
    // Запретить X-Requested-With, т.к. тут CORS через CDN.
    HttpClientConfig(
      baseHeaders = Map.empty,
    )
  }

}

/** Реализация [[IAdvRcvrsMapApi]] с запросом через произвольную ссылку.
  */
class AdvRcvrsMapApiHttpViaUrl(
                                httpClientConfig: () => HttpClientConfig = AdvRcvrsMapApiHttpViaUrl.httpClientConfigDefault,
                              )
  extends IAdvRcvrsMapApi
{

  override def advRcvrsMapJson(args: MRcvrsMapUrlArgs): Future[MGeoNodesResp] = {
    val __mkRoute = routes.controllers.Static.advRcvrsMapJson _

    val route = __mkRoute( args.hashSum )
    val req = HttpReq.routed(
      route = route,
      data  = HttpReqData(
        headers = HttpReqData.headersJsonAccept,
        cache = MHttpCacheInfo(
          // Здесь кэш - контр-аварийный. Т.е. запрос в сеть может уйти, но браузер должен ещё и сам корректно отработать вопрос кэширования.
          // TODO Нужно какое-то железобетонное кэширование, чтобы закэшированная текущая ссылка опрашивалась, потом network, потом аварийный вариант с прошлым кэшем.
          policy     = MHttpCachingPolicies.NetworkFirst,
          rewriteUrl = Some( HttpClient.route2url(__mkRoute(js.undefined)) ),
        ),
        // Запретить X-Requested-With, т.к. тут CORS через CDN.
        // TODO Still need this crunch for browsers? Need to re-investigate, because now (since december-2021) cordova already uses native fetch for this resource (so late, yeah).
        config = HttpClientConfig.baseHeaders.modify(
          _ removed HttpConst.Headers.XRequestedWith.XRW_NAME
        )( httpClientConfig() ),
        // Кукисы не нужны, т.к. запрос обычно идёт через CDN и кэшируется. Кэширование стареющих кукисов может вызывать некоторые сложности.
        credentials = Some(false),
      )
    )

    HttpClient
      .execute(req)
      .respAuthFut
      .successIf200
      .unJson[List[MGeoNodePropsShapes]]
      .map { MGeoNodesResp.apply }
  }

}

