package io.suggest.lk.nodes.form.m

import io.suggest.radio.MRadioData
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.09.2020 14:15
  * Description: LkNodes-состояние обнаружения одного маячка.
  */
case class MNodeBeaconState(
                             data         : MRadioData,
                             isVisible    : Boolean             = true,
                           )


object MNodeBeaconState {

  @inline implicit def univEq: UnivEq[MNodeBeaconState] = UnivEq.derive

  def data = GenLens[MNodeBeaconState](_.data)
  def isVisible = GenLens[MNodeBeaconState](_.isVisible)

}
