package io.suggest.sc.m.dev

import diode.FastEq
import diode.data.Pot
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.2020 8:36
  * Description: Модель описания текущих данных по online-состоянию системы.
  */
object MOnLineS {

  def empty = apply()

  implicit object MOnlineSFeq extends FastEq[MOnLineS] {
    override def eqv(a: MOnLineS, b: MOnLineS): Boolean = {
      (a.state ===* b.state)
    }
  }

  @inline implicit def univEq: UnivEq[MOnLineS] = UnivEq.derive

  def state = GenLens[MOnLineS]( _.state )


  implicit final class MOnlineOpsExt( private val onLine: MOnLineS ) extends AnyVal {

    def isOnline: Boolean = {
      onLine.state.exists(_.hasLink) ||
      onLine.state.isEmpty
    }

  }

}


/** Состояние OnLineAh-контроллера.
  *
  * @param state Pot.empty - не инициализировано.
  *              Pot.pending + empty - инициализация.
  */
case class MOnLineS(
                     state                 : Pot[MOnLineInfo]          = Pot.empty,
                   )
