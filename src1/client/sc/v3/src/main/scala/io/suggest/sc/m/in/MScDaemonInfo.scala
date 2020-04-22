package io.suggest.sc.m.in

import diode.data.Pot
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 22:24
  * Description:
  */
object MScDaemonInfo {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScDaemonInfo] = UnivEq.derive

  def alarmId = GenLens[MScDaemonInfo]( _.alarmId )

}


/** Контейнер состояния демон-контроллера.
  *
  * @param alarmId id интервала-таймера с будильником,
  */
case class MScDaemonInfo(
                          alarmId           : Pot[Int]          = Pot.empty,
                        )
