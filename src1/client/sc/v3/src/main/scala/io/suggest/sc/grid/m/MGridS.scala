package io.suggest.sc.grid.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.sc.sc3.MSc3FindAdsResp
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 21:55
  * Description: Модель состояния плитки.
  *
  * Неявно-пустая модель.
  */
object MGridS {

  def empty = apply()

  /** Поддержка FastEq для [[MGridSFastEq]]. */
  implicit object MGridSFastEq extends FastEq[MGridS] {
    override def eqv(a: MGridS, b: MGridS): Boolean = {
      (a.columnCount ==* b.columnCount) &&
        (a.ads ===* b.ads) &&
        (a.nextReq ===* b.nextReq) &&
        (a.szMult ===* b.szMult)
    }
  }

  implicit def univEq: UnivEq[MGridS] = UnivEq.derive

}


/** Класс модели состояния плитки карточек.
  *
  * @param ads Содержимое плитки.
  * @param nextReq Pot текущего реквеста к серверу за новыми/другими карточками для плитки.
  * @param szMult Мультипликатор размера плитки.
  */
case class MGridS(
                   columnCount    : Int  = 8,    // TODO Явно убрать дефолтовое значение, вместо с empty/ProductEmpty
                   ads            : Seq[MScAdData]        = Nil,
                   nextReq        : Pot[MSc3FindAdsResp]  = Pot.empty,
                   szMult         : MSzMult               = MSzMults.`1.0`
                 )
  extends EmptyProductPot
{

  def withAds(ads: Seq[MScAdData])                = copy(ads = ads)
  def withNextReq(nextReq: Pot[MSc3FindAdsResp])  = copy(nextReq = nextReq)
  def withSzMult(szMult: MSzMult)                 = copy(szMult = szMult)

}
