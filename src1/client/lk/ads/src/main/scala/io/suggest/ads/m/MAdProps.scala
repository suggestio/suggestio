package io.suggest.ads.m

import diode.FastEq
import io.suggest.ads.MLkAdsOneAdResp
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.03.18 18:13
  * Description: Модель данных по одной отображаемой рекламной карточке.
  */
object MAdProps {

  implicit object MAdPropsFastEq extends FastEq[MAdProps] {
    override def eqv(a: MAdProps, b: MAdProps): Boolean = {
      a.adResp ===* b.adResp
    }
  }

  implicit def univEq: UnivEq[MAdProps] = UnivEq.derive

}


case class MAdProps(
                     adResp     : MLkAdsOneAdResp
                   )
