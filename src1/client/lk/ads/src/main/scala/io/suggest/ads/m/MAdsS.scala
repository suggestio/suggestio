package io.suggest.ads.m

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.render.m.MJdRuntime
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:32
  * Description: Модель состояния текущего узла.
  */
object MAdsS {

  implicit object MAdsSFastEq extends FastEq[MAdsS] {
    override def eqv(a: MAdsS, b: MAdsS): Boolean = {
      (a.jdRuntime ===* b.jdRuntime) &&
      (a.ads ===* b.ads) &&
      (a.hasMoreAds ==* b.hasMoreAds)
    }
  }

  @inline implicit def univEq: UnivEq[MAdsS] = UnivEq.derive

  val ads = GenLens[MAdsS](_.ads)

}


/** Контейнер данных по текущему узлу, на котором открыта форм.
  */
case class MAdsS(
                  jdRuntime  : MJdRuntime,
                  ads        : Pot[Vector[MAdProps]]  = Pot.empty,
                  hasMoreAds : Boolean                = true
                )
