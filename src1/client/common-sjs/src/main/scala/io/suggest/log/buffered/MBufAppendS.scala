package io.suggest.log.buffered

import diode.data.Pot
import io.suggest.log.MLogMsg
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

import scala.scalajs.js.timers.SetTimeoutHandle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.04.2020 17:08
  * Description: Модель buffered-логгера.
  */
object MBufAppendS {

  @inline implicit def univEq: UnivEq[MBufAppendS] = UnivEq.derive

  def accRev = GenLens[MBufAppendS]( _.accRev )
  def expTimerId = GenLens[MBufAppendS]( _.expTimerId )

}


/** Контейнер данных состояния аппендера.
  *
  * @param accRev Обратный аккамулятор наборов лог-сообщений.
  * @param expTimerId id таймера сброса накопленных сообщений.
  */
case class MBufAppendS(
                        accRev          : List[Seq[MLogMsg]]         = Nil,
                        expTimerId      : Pot[SetTimeoutHandle]      = Pot.empty,
                      )
