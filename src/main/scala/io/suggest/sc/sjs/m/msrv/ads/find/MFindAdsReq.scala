package io.suggest.sc.sjs.m.msrv.ads.find

import io.suggest.sc.ScConstants
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msc.fsm.{MScStateT, MScFsm}
import io.suggest.sc.sjs.m.msrv.MSrv

import scala.scalajs.js.{Any, Dictionary}
import io.suggest.ad.search.AdSearchConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 14:13
 * Description: Модель qs-аргументов для запроса к findAds.
 * Объект будет сериализован в URL js-роутером, который рендериться на сервере.
 */

trait MFindAdsReq {

  def producerId  : Option[String]
  def catId       : Option[String]
  def levelId     : Option[String]
  def ftsQuery    : Option[String]
  def limit       : Option[Int]
  def offset      : Option[Int]
  def receiverId  : Option[String]
  def firstAdId   : Option[String]
  def generation  : Option[Long]
  def geo         : Option[IMGeoMode]
  def screenInfo  : Option[IMScreen]

  /** Версия API. Она по идее не меняется. */
  def apiVsn      : Int = MSrv.API_VSN

  /** Собрать итоговый json для передачи в router. */
  def toJson: Dictionary[Any] = {
    val d = Dictionary[Any](
      API_VSN_FN -> apiVsn
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
    if (geo.nonEmpty)
      d.update(GEO_MODE_FN, geo.get.toQsStr)
    if (screenInfo.nonEmpty)
      d.update(SCREEN_INFO_FN, screenInfo.get.toQsValue)

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
  override def firstAdId   : Option[String]    = None
  override def generation  : Option[Long]      = None
  override def geo   : Option[IMGeoMode] = None
  override def screenInfo  : Option[IMScreen]  = None
}

/** Враппер для заворачивания другой реализации [[MFindAdsReq]]. */
trait MFindAdsReqWrapper extends MFindAdsReq {
  def _underlying: MFindAdsReq

  override def producerId   = _underlying.producerId
  override def limit        = _underlying.limit
  override def firstAdId    = _underlying.firstAdId
  override def screenInfo   = _underlying.screenInfo
  override def catId        = _underlying.catId
  override def levelId      = _underlying.levelId
  override def receiverId   = _underlying.receiverId
  override def offset       = _underlying.offset
  override def geo    = _underlying.geo
  override def ftsQuery     = _underlying.ftsQuery
  override def generation   = _underlying.generation
}


/** Дефолтовая реализация, обычно она используется. */
trait MFindAdsReqDflt extends MFindAdsReq {
  def _mgs: MGridState
  def _fsmState: MScStateT = MScFsm.state

  override def receiverId: Option[String]   = _fsmState.rcvrAdnId
  override def limit : Option[Int]          = Some(_mgs.adsPerLoad)
  override def offset: Option[Int]          = Some(_mgs.blocksLoaded)
  override def ftsQuery: Option[String]     = _fsmState.ftsSearch

  override def levelId: Option[String] = {
    import ScConstants.ShowLevels._
    val st = MScFsm.state
    if (st.cat.nonEmpty) {
      Some(ID_CATS)
    } else if (st.ftsSearch.nonEmpty) {
      None
    } else {
      Some(ID_START_PAGE)
    }
  }

  override def catId: Option[String] = {
    MScFsm.state.cat.map(_.catId)
  }

}

