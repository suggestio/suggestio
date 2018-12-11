package io.suggest.sc.u.api

import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.{MSc3Resp, MScQs}
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.sjs.common.xhr.{HttpReq, HttpReqData, Xhr}
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json
import io.suggest.ueq.UnivEqUtil._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.05.18 21:38
  * Description: Поддержка sc UniApi.
  */

object ScUniApi {

  /** Через сколько секунд запрос можно закрывать по таймауту? */
  def REQ_TIMEOUT_MS = Some(20000)

  /** До сколько нормализовывать координату. Максимум - 5 знаков. */
  def CACHE_NORM_COORD_BY = 4


  /** Сборка ссылки для набора qs. */
  def scQs2Route(scQs: MScQs): Route = {
    ScJsRoutes.controllers.Sc.pubApi(
      PlayJsonSjsUtil.toNativeJsonObj(
        Json.toJsObject( scQs )
      )
    )
  }

  /** Отрезать или укоротить элементы qs для сохранения в кэше.
    *
    * @param scQs Исходные QS.
    * @return Опциональные qs.
    *         None - значит ничего не изменилось.
    */
  def stripQsForCaching(scQs: MScQs): Option[MScQs] = {
    var scQs2 = scQs

    // Удалить gen random:
    if (scQs2.search.genOpt.nonEmpty) {
      scQs2 = scQs2.withSearch(
        scQs2.search.withGenOpt(None)
      )
    }

    // Нормализовать гео-координаты до нескольких цифр после запятой: 4 цифры достаточно для кэша.
    if (scQs2.common.locEnv.geoLocOpt.nonEmpty) {
      scQs2 = scQs2.withCommon(
        scQs2.common.withLocEnv(
          scQs2.common.locEnv.withGeoLocOpt {
            val mloc0 = scQs2.common.locEnv.geoLocOpt.get
            val mloc2 = mloc0.copy(
              point = mloc0.point.withCoordScale( MGeoPoint.FracScale.NEAR ),
              accuracyOptM = None,
            )
            Some(mloc2)
          }
        )
      )
    }

    OptionUtil.maybe(scQs !===* scQs2)(scQs2)
  }

}


/** Интерфейс абстрактного API. */
trait IScUniApi {

  def pubApi(scQs: MScQs): Future[MSc3Resp]

}


/** Реализация поддержки Sc UniApi поверх HTTP. */
trait ScUniApiHttpImpl extends IScUniApi {

  override def pubApi(scQs: MScQs): Future[MSc3Resp] = {
    // Сборка реквеста. В API выдачи все реквесты считают кэшируемыми.
    val req = HttpReq.routed(
      route = ScUniApi.scQs2Route(scQs),
      data  = HttpReqData(
        headers   = HttpReqData.headersJsonAccept,
        timeoutMs = ScUniApi.REQ_TIMEOUT_MS,
        cache = MHttpCacheInfo(
          policy = MHttpCachingPolicies.NetworkFirst,
          // Кэшировать следует по оптимизированной ссылке, в которой отсутствуют всякие неважные типа gen, чтобы в аварийной ситуации долго не думать.
          rewriteUrl = for {
            scQs4Caching <- ScUniApi.stripQsForCaching(scQs)
          } yield {
            val route = ScUniApi.scQs2Route(scQs4Caching)
            Xhr.route2url(route)
          }
        )
      )
    )
    Xhr.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MSc3Resp]
  }

}
