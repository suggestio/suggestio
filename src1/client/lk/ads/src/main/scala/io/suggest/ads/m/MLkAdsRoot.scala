package io.suggest.ads.m

import diode.FastEq
import io.suggest.ads.MLkAdsConf
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:22
  * Description: Корневая модель circuit-состояния [[io.suggest.ads.LkAdsCircuit]].
  */
object MLkAdsRoot {

  implicit object MLkAdsRootFastEq extends FastEq[MLkAdsRoot] {
    override def eqv(a: MLkAdsRoot, b: MLkAdsRoot): Boolean = {
      (a.ads ===* b.ads) &&
      (a.conf ===* b.conf)
    }
  }

  @inline implicit def univEq: UnivEq[MLkAdsRoot] = UnivEq.derive

  val conf = GenLens[MLkAdsRoot](_.conf)
  val ads = GenLens[MLkAdsRoot](_.ads)

}


/** Класс root-модели состояния react-формы управления карточками.
  *
  * @param ads Модель данных состояния по текущему узлу.
  */
case class MLkAdsRoot(
                       conf         : MLkAdsConf,
                       ads          : MAdsS
                     )
