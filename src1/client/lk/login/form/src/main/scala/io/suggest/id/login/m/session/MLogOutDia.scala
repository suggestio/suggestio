package io.suggest.id.login.m.session

import diode.data.Pot
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.2020 17:14
  * Description: Модель состояния диалога выхода из системы.
  */
object MLogOutDia {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MLogOutDia] = UnivEq.derive

  def req = GenLens[MLogOutDia]( _.req )

}


case class MLogOutDia(
                       req          : Pot[None.type]              = Pot.empty,
                     )
