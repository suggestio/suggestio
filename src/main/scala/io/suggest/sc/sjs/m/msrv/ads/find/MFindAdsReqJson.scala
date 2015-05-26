package io.suggest.sc.sjs.m.msrv.ads.find

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.msrv.MSrv

import scala.scalajs.js.{Object, Any, Dictionary}
import io.suggest.ad.search.AdSearchConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 14:13
 * Description: Модель qs-аргументов для запроса к findAds.
 * Объект будет сериализован в URL js-роутером, который рендериться на сервере.
 */
sealed trait MFindAdsReqJson extends Object


object MFindAdsReqJson {

  /**
   * Сборка экземпляра модели для последующей передачи его в js-роутер.
   * @return json dictionary, представленный как экземпляр [[MFindAdsReqJson]].
   */
  def apply(producerId  : Option[String]    = None,
            catId       : Option[String]    = None,
            levelId     : Option[String]    = None,
            ftsQuery    : Option[String]    = None,
            limit       : Option[Int]       = None,
            offset      : Option[Int]       = None,
            receiverId  : Option[String]    = None,
            firstAdId   : Option[String]    = None,
            generation  : Option[Long]      = None,
            geoModeFn   : Option[IMGeoMode] = None,
            screenInfo  : Option[IMScreen]  = None
           ): MFindAdsReqJson = {

    val d = Dictionary[Any](
      API_VSN_FN -> MSrv.apiVsn
    )

    if (producerId.nonEmpty)
      d.update(PRODUCER_ID_FN, producerId.get)
    if (catId.nonEmpty)
      d.update(CAT_ID_FN, catId.get)
    if (levelId.nonEmpty)
      d.update(LEVEL_ID_FN, levelId.get)
    if (ftsQuery.nonEmpty)
      d.update(FTS_QUERY_FN, ftsQuery.get)
    if (limit.isDefined)
      d.update(RESULTS_LIMIT_FN, limit.get)
    if (offset.isDefined)
      d.update(RESULTS_OFFSET_FN, offset.get)
    if (receiverId.nonEmpty)
      d.update(RECEIVER_ID_FN, receiverId.get)
    if (firstAdId.nonEmpty)
      d.update(FIRST_AD_ID_FN, firstAdId.get)
    if (generation.nonEmpty)
      d.update(GENERATION_FN, generation.get)
    if (geoModeFn.nonEmpty)
      d.update(GEO_MODE_FN, geoModeFn.get.toQsStr)
    if (screenInfo.nonEmpty)
      d.update(SCREEN_INFO_FN, screenInfo.get.toQsValue)

    d.asInstanceOf[MFindAdsReqJson]
  }

}
