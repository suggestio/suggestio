package io.suggest.ads.m

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.render.v.JdCss
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:32
  * Description: Модель состояния текущего узла.
  */
object MAdsS {

  implicit object MAdsSFastEq extends FastEq[MAdsS] {
    override def eqv(a: MAdsS, b: MAdsS): Boolean = {
      (a.jdCss ===* b.jdCss) &&
        (a.ads ===* b.ads) &&
        (a.hasMoreAds ==* b.hasMoreAds)
    }
  }

  implicit def univEq: UnivEq[MAdsS] = UnivEq.derive

}


/** Контейнер данных по текущему узлу, на котором открыта форм.
  */
case class MAdsS(
                  jdCss      : JdCss,
                  ads        : Pot[Vector[MAdProps]]  = Pot.empty,
                  hasMoreAds : Boolean                = true
                ) {

  def withJdCss(jdCss: JdCss)                     = copy(jdCss = jdCss)
  def withAds(ads: Pot[Vector[MAdProps]])         = copy(ads = ads)
  def withHasMoreAds(hasMoreAds: Boolean)         = copy(hasMoreAds = hasMoreAds)

}
