package io.suggest.ads.m

import diode.FastEq
import diode.data.Pot
import io.suggest.ads.MLkAdsOneAdResp
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:32
  * Description: Модель состояния текущего узла.
  */
object MCurrNodeS {

  implicit object MCurrNodeSFastEq extends FastEq[MCurrNodeS] {
    override def eqv(a: MCurrNodeS, b: MCurrNodeS): Boolean = {
      (a.nodeKey ===* b.nodeKey) &&
        (a.ads ===* b.ads) &&
        (a.hasMoreAds ==* b.hasMoreAds)
    }
  }

  implicit def univEq: UnivEq[MCurrNodeS] = UnivEq.derive

}


/** Контейнер данных по текущему узлу, на котором открыта форм.
  *
  * @param nodeKey Ключ до текущего узла.
  */
case class MCurrNodeS(
                       nodeKey    : RcvrKey,
                       ads        : Pot[Vector[MLkAdsOneAdResp]] = Pot.empty,
                       hasMoreAds : Boolean = true
                     ) {

  def withNodeKey(nodeKey: RcvrKey)               = copy(nodeKey = nodeKey)
  def withAds(ads: Pot[Vector[MLkAdsOneAdResp]])  = copy(ads = ads)
  def withHasMoreAds(hasMoreAds: Boolean)         = copy(hasMoreAds = hasMoreAds)

}
