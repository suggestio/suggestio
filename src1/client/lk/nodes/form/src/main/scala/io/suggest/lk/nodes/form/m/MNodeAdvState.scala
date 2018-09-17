package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 10:33
  * Description: Модель данных состояния размещения карточки на указанном узле.
  */
object MNodeAdvState {

  @inline implicit def univEq: UnivEq[MNodeAdvState] = UnivEq.force

}

case class MNodeAdvState(
                          newIsEnabledPot               : Pot[Boolean]  = Pot.empty,
                          isShowOpenedPot   : Pot[Boolean]  = Pot.empty
                        ) {

  def withReq(req2: Pot[Boolean]) = copy(newIsEnabledPot = req2)

  def withIsShowOpenedPot(isShowOpenedPot: Pot[Boolean])    = copy(isShowOpenedPot = isShowOpenedPot)

  def isShowOpened = isShowOpenedPot.getOrElse(false)
  def newIsEnabled = newIsEnabledPot.getOrElse(false)


}
