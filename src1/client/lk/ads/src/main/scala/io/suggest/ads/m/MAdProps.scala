package io.suggest.ads.m

import diode.FastEq
import diode.data.Pot
import io.suggest.ads.MLkAdsOneAdResp
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.lk.nodes.MLknNode
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.03.18 18:13
  * Description: Модель данных по одной отображаемой рекламной карточке.
  */
object MAdProps {

  implicit object MAdPropsFastEq extends FastEq[MAdProps] {
    override def eqv(a: MAdProps, b: MAdProps): Boolean = {
      (a.adResp ===* b.adResp) &&
      (a.shownAtParentReq ===* b.shownAtParentReq)
    }
  }

  @inline implicit def univEq: UnivEq[MAdProps] = UnivEq.derive

  def shownAtParentReq = GenLens[MAdProps](_.shownAtParentReq)

}


/** Контейнер данных по одной карточке.
  *
  * @param adResp Исходный ответ сервера по карточке.
  * @param shownAtParentReq Pot процедуры обновления галочки размещения на родительском узле.
  */
case class MAdProps(
                     adResp             : MLkAdsOneAdResp,
                     shownAtParentReq   : Pot[MLknNode] = Pot.empty
                   ) {

  val jdDataJs = MJdDataJs.fromJdData( adResp.jdAdData )

}
