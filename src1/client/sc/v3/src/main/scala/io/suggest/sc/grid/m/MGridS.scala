package io.suggest.sc.grid.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdConf
import io.suggest.jd.render.v.JdCss
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

  /** Поддержка FastEq для [[MGridSFastEq]]. */
  implicit object MGridSFastEq extends FastEq[MGridS] {
    override def eqv(a: MGridS, b: MGridS): Boolean = {
      (a.jdConf       ==*  b.jdConf) &&
        (a.jdCss      ===* b.jdCss) &&
        (a.ads        ===* b.ads) &&
        (a.hasMoreAds ==*  b.hasMoreAds) &&
        (a.gridSz     ===* b.gridSz)
    }
  }

  implicit def univEq: UnivEq[MGridS] = UnivEq.derive

}


/** Класс модели состояния плитки карточек.
  *
  * @param ads Содержимое плитки.
  *            Pot реквеста к серверу за новыми карточками для плитки.
  *            Необходимо (см пул коннекшенов в GridR) как минимум IndexedSeq, которая по дефолту является Vector.
  * @param gridSz Реально-занимаемый размер плитки. Вычисляется во время раскладывания карточек.
  */
case class MGridS(
                   jdConf         : MJdConf,
                   jdCss          : JdCss,
                   ads            : Pot[Vector[MScAdData]]        = Pot.empty,
                   hasMoreAds     : Boolean                       = true,
                   gridSz         : Option[MSize2di]              = None
                 )
  extends EmptyProductPot
{

  def withJdConf(jdConf: MJdConf)                         = copy(jdConf = jdConf)
  def withJdCss(jdCss: JdCss)                             = copy(jdCss = jdCss)
  def withAds(ads: Pot[Vector[MScAdData]])                = copy(ads = ads)
  def withHasMoreAds(hasMoreAds: Boolean)                 = copy(hasMoreAds = hasMoreAds)
  def withGridSz(realContentSz: Option[MSize2di])         = copy(gridSz = realContentSz)

}
