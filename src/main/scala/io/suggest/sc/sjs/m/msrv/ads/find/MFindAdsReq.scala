package io.suggest.sc.sjs.m.msrv.ads.find

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msrv.ToJsonWithApiVsnT

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
  def levelId     : Option[String]
  def ftsQuery    : Option[String]
  def limit       : Option[Int]
  def offset      : Option[Int]
  def receiverId  : Option[String]
  def firstAdIds  : Seq[String]
  def generation  : Option[Long]
  def geo         : Option[IMGeoMode]
  def screenInfo  : Option[IMScreen]
  def withoutId   : Option[String]

  /** Собрать итоговый json для передачи в router. */
  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    for (prodId <- producerId)
      d(PRODUCER_ID_FN) = prodId
    for (_levelId <- levelId)
      d(LEVEL_ID_FN) = _levelId
    for (q <- ftsQuery)
      d(FTS_QUERY_FN) = q
    for (_limit <- limit)
      d(RESULTS_LIMIT_FN) = _limit
    for (off <- offset)
      d(RESULTS_OFFSET_FN) = off
    for (rcvrId <- receiverId)
      d(RECEIVER_ID_FN) = rcvrId
    if (firstAdIds.nonEmpty)
      d(FIRST_AD_ID_FN) = firstAdIds.toJSArray
    for (gen <- generation)
      d(GENERATION_FN) = gen
    for (_geo <- geo)
      d(GEO_MODE_FN) = _geo.toQsStr
    for (scrInfo <- screenInfo)
      d(SCREEN_INFO_FN) = scrInfo.toQsValue
    for (woId <- withoutId)
      d(WITHOUT_IDS_FN) = woId

    d
  }

}


/** Задефолченная реализация [[MFindAdsReq]]. */
trait MFindAdsReqEmpty extends MFindAdsReq {
  override def producerId  : Option[String]    = None
  override def levelId     : Option[String]    = None
  override def ftsQuery    : Option[String]    = None
  override def limit       : Option[Int]       = None
  override def offset      : Option[Int]       = None
  override def receiverId  : Option[String]    = None
  override def firstAdIds  : Seq[String]       = Nil
  override def generation  : Option[Long]      = None
  override def geo         : Option[IMGeoMode] = None
  override def screenInfo  : Option[IMScreen]  = None
  override def withoutId   : Option[String]    = None
}


/** Враппер для заворачивания другой реализации [[MFindAdsReq]]. */
trait MFindAdsReqWrapper extends MFindAdsReq {
  def _underlying: MFindAdsReq

  override def producerId   = _underlying.producerId
  override def levelId      = _underlying.levelId
  override def ftsQuery     = _underlying.ftsQuery
  override def limit        = _underlying.limit
  override def offset       = _underlying.offset
  override def receiverId   = _underlying.receiverId
  override def firstAdIds   = _underlying.firstAdIds
  override def generation   = _underlying.generation
  override def geo          = _underlying.geo
  override def screenInfo   = _underlying.screenInfo
  override def withoutId    = _underlying.withoutId
}


/** Дефолтовая реализация, обычно она используется. */
trait MFindAdsReqDflt extends MFindAdsReq {
  def _mgs: MGridState

  override def limit : Option[Int]          = Some(_mgs.adsPerLoad)
  override def offset: Option[Int]          = Some(_mgs.blocksLoaded)

}

