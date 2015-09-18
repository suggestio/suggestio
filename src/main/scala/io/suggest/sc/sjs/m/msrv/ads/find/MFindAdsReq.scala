package io.suggest.sc.sjs.m.msrv.ads.find

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msrv.{ToJsonWithApiVsnT, MSrv}

import scala.scalajs.js.{Any, Dictionary}
import io.suggest.ad.search.AdSearchConstants._
import scala.scalajs.js.JSConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 14:13
 * Description: Модель qs-аргументов для запроса к findAds.
 * Объект будет сериализован в URL js-роутером, который рендериться на сервере.
 */

trait MFindAdsReq extends ToJsonWithApiVsnT {

  def producerId  : Option[String]
  def catId       : Option[String]
  def levelId     : Option[String]
  def ftsQuery    : Option[String]
  def limit       : Option[Int]
  def offset      : Option[Int]
  def receiverId  : Option[String]
  def firstAdIds  : Seq[String]
  def generation  : Option[Long]
  def geo         : Option[IMGeoMode]
  def screenInfo  : Option[IMScreen]

  /** Собрать итоговый json для передачи в router. */
  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    if (producerId.nonEmpty)
      d(PRODUCER_ID_FN) = producerId.get
    if (catId.nonEmpty)
      d(CAT_ID_FN) = catId.get
    if (levelId.nonEmpty)
      d(LEVEL_ID_FN) = levelId.get
    if (ftsQuery.nonEmpty)
      d(FTS_QUERY_FN) = ftsQuery.get
    if (limit.isDefined)
      d(RESULTS_LIMIT_FN) = limit.get
    if (offset.isDefined)
      d(RESULTS_OFFSET_FN) = offset.get
    if (receiverId.nonEmpty)
      d(RECEIVER_ID_FN) = receiverId.get
    if (firstAdIds.nonEmpty)
      d(FIRST_AD_ID_FN) = firstAdIds.toJSArray
    if (generation.nonEmpty)
      d(GENERATION_FN) = generation.get
    if (geo.nonEmpty)
      d(GEO_MODE_FN) = geo.get.toQsStr
    if (screenInfo.nonEmpty)
      d(SCREEN_INFO_FN) = screenInfo.get.toQsValue

    d
  }

}

/** Задефолченная реализация [[MFindAdsReq]]. */
trait MFindAdsReqEmpty extends MFindAdsReq {
  override def producerId  : Option[String]    = None
  override def catId       : Option[String]    = None
  override def levelId     : Option[String]    = None
  override def ftsQuery    : Option[String]    = None
  override def limit       : Option[Int]       = None
  override def offset      : Option[Int]       = None
  override def receiverId  : Option[String]    = None
  override def firstAdIds  : Seq[String]       = Nil
  override def generation  : Option[Long]      = None
  override def geo         : Option[IMGeoMode] = None
  override def screenInfo  : Option[IMScreen]  = None
}

/** Враппер для заворачивания другой реализации [[MFindAdsReq]]. */
trait MFindAdsReqWrapper extends MFindAdsReq {
  def _underlying: MFindAdsReq

  override def producerId   = _underlying.producerId
  override def catId        = _underlying.catId
  override def levelId      = _underlying.levelId
  override def ftsQuery     = _underlying.ftsQuery
  override def limit        = _underlying.limit
  override def offset       = _underlying.offset
  override def receiverId   = _underlying.receiverId
  override def firstAdIds   = _underlying.firstAdIds
  override def generation   = _underlying.generation
  override def geo          = _underlying.geo
  override def screenInfo   = _underlying.screenInfo
}


/** Дефолтовая реализация, обычно она используется. */
trait MFindAdsReqDflt extends MFindAdsReq {
  def _mgs: MGridState

  override def limit : Option[Int]          = Some(_mgs.adsPerLoad)
  override def offset: Option[Int]          = Some(_mgs.blocksLoaded)

}

