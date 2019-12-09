package io.suggest.sc.u.api

import io.suggest.common.empty.OptionUtil
import io.suggest.geo.{MGeoLoc, MGeoPoint, MLocEnv}
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.client.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.proto.http.model.{Route, _}
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.ads.MAdsSearchReq
import io.suggest.sc.sc3.{MSc3Resp, MScCommonQs, MScQs}
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json
import io.suggest.ueq.UnivEqUtil._
import monocle.Traversal

import scala.concurrent.Future
import scalaz.std.option._

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
    var updateFuns = List.empty[MScQs => MScQs]

    // Удалить gen random:
    if (scQs.search.genOpt.nonEmpty) {
      updateFuns ::= MScQs.search
        .composeLens( MAdsSearchReq.genOpt )
        .set( None )
    }

    // Нормализовать гео-координаты до нескольких цифр после запятой: 4 цифры достаточно для кэша.
    val qs_common_locEnv_geoLoc_LENS = MScQs.common
      .composeLens( MScCommonQs.locEnv )
      .composeLens( MLocEnv.geoLocOpt )

    for {
      mloc0 <- qs_common_locEnv_geoLoc_LENS.get( scQs )
    } {
      var glModF = MGeoLoc.point
        .modify( _.withCoordScale(MGeoPoint.FracScale.NEAR) )

      if (mloc0.accuracyOptM.nonEmpty)
        glModF = glModF andThen MGeoLoc.accuracyOptM.set( None )

      updateFuns ::= qs_common_locEnv_geoLoc_LENS
        .composeTraversal( Traversal.fromTraverse[Option, MGeoLoc] )
        .modify( glModF )
    }

    val scQs2 =
      if (updateFuns.isEmpty) scQs
      else updateFuns.reduceLeft(_ andThen _)(scQs)

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
    HttpClient.execute(
      HttpReq.routed(
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
              HttpClient.route2url(route)
            }
          )
        )
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MSc3Resp]
  }

}
