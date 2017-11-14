package io.suggest.sc.grid.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.jd.MJdAdData
import io.suggest.sc.sc3.MSc3FindAdsResp
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 21:55
  * Description: Модель состояния плитки.
  *
  * Неявно-пустая модель.
  */
object MGridS {

  /** Поддержка FastEq для [[MGridSFastEq]]. */
  implicit object MGridSFastEq extends FastEq[MGridS] {
    override def eqv(a: MGridS, b: MGridS): Boolean = {
      (a.ads ===* b.ads) &&
        (a.nextReq ===* b.nextReq)
    }
  }

  implicit def univEq: UnivEq[MGridS] = UnivEq.derive

}


/** Класс модели состояния плитки карточек.
  *
  * @param ads Содержимое плитки.
  * @param nextReq Pot текущего реквеста к серверу за новыми/другими карточками для плитки.
  */
case class MGridS(
                   ads        : Seq[MJdAdData]        = Nil,
                   nextReq    : Pot[MSc3FindAdsResp]  = Pot.empty
                 )
  extends EmptyProductPot
{

  def withAds(ads: Seq[MJdAdData]) = copy(ads = ads)
  def withNextReq(nextReq: Pot[MSc3FindAdsResp])  = copy(nextReq = nextReq)

}
