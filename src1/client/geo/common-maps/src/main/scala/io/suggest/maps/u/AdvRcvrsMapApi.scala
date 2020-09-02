package io.suggest.maps.u

import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.client.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.proto.http.model._
import io.suggest.routes.{IJsRouter, routes}
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


/** Реализация [[IAdvRcvrsMapApi]] с запросом через произвольную ссылку.
  * @param jsRoutes Функция, возвращающая инстанс js-роутера.
  *                 Напрямую использовать инстанс пока нельзя: cordova-ios выбрасывает undefined слишком рано.
  *                 Надо унифицировать js-роутеры, и можно будет убрать этот параметр полностью.
  */
class AdvRcvrsMapApiHttpViaUrl(jsRoutes: => IJsRouter = routes) extends IAdvRcvrsMapApi {

  override def advRcvrsMapJson(args: MRcvrsMapUrlArgs): Future[MGeoNodesResp] = {
    val __mkRoute = jsRoutes.controllers.Static.advRcvrsMapJson _

    val req = HttpReq.routed(
      route = __mkRoute( args.hashSum ),
      data  = HttpReqData(
        headers = HttpReqData.headersJsonAccept,
        cache = MHttpCacheInfo(
          // Здесь кэш - контр-аварийный. Т.е. запрос в сеть может уйти, но браузер должен ещё и сам корректно отработать вопрос кэширования.
          // TODO Нужно какое-то железобетонное кэширование, чтобы закэшированная текущая ссылка опрашивалась, потом network, потом аварийный вариант с прошлым кэшем.
          policy     = MHttpCachingPolicies.NetworkFirst,
          rewriteUrl = Some( HttpClient.route2url(__mkRoute(js.undefined)) ),
        ),
        // Запретить X-Requested-With, т.к. тут CORS через CDN.
        config = HttpClientConfig(
          baseHeaders = Map.empty,
        ),
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

