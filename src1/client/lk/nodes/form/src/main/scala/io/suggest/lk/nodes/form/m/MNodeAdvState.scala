package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.lk.nodes.MLknAdv
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.spa.DiodeUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 10:33
  * Description: Модель данных состояния размещения карточки на указанном узле.
  */
object MNodeAdvState {

  @inline implicit def univEq: UnivEq[MNodeAdvState] = UnivEq.force

  def newIsEnabledPot = GenLens[MNodeAdvState](_.newIsEnabledPot)
  def isShowOpenedPot = GenLens[MNodeAdvState](_.isShowOpenedPot)
  def alwaysOutlined  = GenLens[MNodeAdvState](_.alwaysOutlinedPot)

  def empty = apply()

  def from(lknAdv: MLknAdv): MNodeAdvState = {
    MNodeAdvState(
      newIsEnabledPot = Pot.empty.ready( lknAdv.hasAdv ),
      isShowOpenedPot = Pot.empty.ready( lknAdv.advShowOpened ),
      alwaysOutlinedPot = Pot.empty.ready( lknAdv.alwaysOutlined ),
    )
  }
  def from(lknAdvOpt: Option[MLknAdv]): MNodeAdvState = {
    lknAdvOpt.fold(empty)(from)
  }

}


case class MNodeAdvState(
                          newIsEnabledPot   : Pot[Boolean]  = Pot.empty,
                          isShowOpenedPot   : Pot[Boolean]  = Pot.empty,
                          alwaysOutlinedPot : Pot[Boolean]  = Pot.empty,
                        )
