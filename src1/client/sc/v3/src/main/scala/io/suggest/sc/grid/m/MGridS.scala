package io.suggest.sc.grid.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.sc.sc3.{MSc3AdsResp, MSc3Resp}
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
      (a.columnsCount ==* b.columnsCount) &&
        (a.ads ===* b.ads) &&
        (a.hasMoreAds ==* b.hasMoreAds) &&
        (a.nextReq ===* b.nextReq) &&
        (a.gridSz ===* b.gridSz) &&
        (a.szMult ===* b.szMult) &&
        (a.blockClickReq ===* b.blockClickReq)
    }
  }

  implicit def univEq: UnivEq[MGridS] = UnivEq.derive

}


/** Класс модели состояния плитки карточек.
  *
  * @param columnsCount Кол-во колонок сетки.
  * @param ads Содержимое плитки.
  * @param nextReq Pot текущего реквеста к серверу за новыми/другими карточками для плитки.
  * @param szMult Мультипликатор размера плитки.
  * @param gridSz Реально-занимаемый размер плитки. Вычисляется во время раскладывания карточек.
  * @param blockClickReq Контейнер состояния запроса за focused-карточками выдачи.
  */
case class MGridS(
                   columnsCount   : Int                   = 8, // TODO Явно убрать дефолтовое значение, вместо с empty/ProductEmpty
                   ads            : Seq[MScAdData]        = Nil,
                   hasMoreAds     : Boolean               = true,
                   nextReq        : Pot[MSc3AdsResp]  = Pot.empty,
                   gridSz         : Option[MSize2di]      = None,
                   szMult         : MSzMult               = MSzMults.`1.0`,
                   blockClickReq  : Pot[MSc3Resp]         = Pot.empty
                 )
  extends EmptyProductPot
{

  def withColumnsCount(columnsCount: Int)                 = copy(columnsCount = columnsCount)
  def withAds(ads: Seq[MScAdData])                        = copy(ads = ads)
  def withHasMoreAds(hasMoreAds: Boolean)                 = copy(hasMoreAds = hasMoreAds)
  def withNextReq(nextReq: Pot[MSc3AdsResp])          = copy(nextReq = nextReq)
  def withGridSz(realContentSz: Option[MSize2di])         = copy(gridSz = realContentSz)
  def withSzMult(szMult: MSzMult)                         = copy(szMult = szMult)
  def withBlockClickReq(blockClickReq: Pot[MSc3Resp])     = copy(blockClickReq = blockClickReq)

}
