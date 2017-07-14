package io.suggest.sc.sjs.m.msrv.tile

import io.suggest.ad.search.AdSearchConstants._
import io.suggest.dev.MScreen
import io.suggest.geo.{MLocEnv, MLocEnvJs}
import io.suggest.sc.sjs.m.msrv.ToJsonWithApiVsnT

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 14:13
 * Description: Модель qs-аргументов для запроса к findAds.
 * Объект будет сериализован в URL js-роутером, который рендериться на сервере.
 */

trait MFindAdsReq extends ToJsonWithApiVsnT {

  def producerId  : Option[String]
  def limit       : Option[Int]
  def offset      : Option[Int]
  def receiverId  : Option[String]
  def generation  : Option[Long]
  def locEnv      : MLocEnv
  def screenInfo  : Option[MScreen]
  def tagNodeId   : Option[String]

  /** Собрать итоговый json для передачи в router. */
  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    for (prodId <- producerId)
      d(PRODUCER_ID_FN) = prodId
    for (_limit <- limit)
      d(LIMIT_FN) = _limit
    for (off <- offset)
      d(OFFSET_FN) = off
    for (rcvrId <- receiverId)
      d(RECEIVER_ID_FN) = rcvrId
    for (gen <- generation)
      d(GENERATION_FN) = gen

    val _le = locEnv
    if ( _le.nonEmpty )
      d(LOC_ENV_FN) = MLocEnvJs.toJson(_le)

    for (scrInfo <- screenInfo)
      d(SCREEN_INFO_FN) = scrInfo.toQsValue
    for (_tagNodeId <- tagNodeId)
      d(TAG_NODE_ID_FN) = _tagNodeId

    d
  }

}


/** Задефолченная реализация [[MFindAdsReq]]. */
trait MFindAdsReqDflt extends MFindAdsReq {
  override def producerId  : Option[String]    = None
  override def limit       : Option[Int]       = None
  override def offset      : Option[Int]       = None
  override def receiverId  : Option[String]    = None
  override def generation  : Option[Long]      = None
  override def locEnv      : MLocEnv           = MLocEnv.empty
  override def screenInfo  : Option[MScreen]  = None
  override def tagNodeId   : Option[String] = None
}


/** Враппер для заворачивания другой реализации [[MFindAdsReq]]. */
trait MFindAdsReqWrapper extends MFindAdsReq {
  def _underlying: MFindAdsReq

  override def producerId   = _underlying.producerId
  override def limit        = _underlying.limit
  override def offset       = _underlying.offset
  override def receiverId   = _underlying.receiverId
  override def generation   = _underlying.generation
  override def locEnv       = _underlying.locEnv
  override def screenInfo   = _underlying.screenInfo
  override def tagNodeId    = _underlying.tagNodeId
}
