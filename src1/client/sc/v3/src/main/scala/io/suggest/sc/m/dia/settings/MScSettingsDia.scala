package io.suggest.sc.m.dia.settings

import diode.data.Pot
import io.suggest.sc.sc3.MScSettingsData
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.2020 18:42
  * Description: Модель состояния диалога настроек выдачи.
  *
  * Исторически, сами настроек могут жить за пределами состояния диалога.
  * Например, настройками bluetooth заведует BeaconerAh со своим собственным состоянием.
  */
object MScSettingsDia {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScSettingsDia] = UnivEq.derive

  def opened = GenLens[MScSettingsDia]( _.opened )
  def data = GenLens[MScSettingsDia]( _.data )

}

case class MScSettingsDia(
                           opened       : Boolean                 = false,
                           data         : Pot[MScSettingsData]    = Pot.empty,
                         )
