package io.suggest.sc.model.search

import diode.{FastEq, UseValueEq}
import io.suggest.sc.model.inx.MapReIndex
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

import scala.scalajs.js.timers.SetTimeoutHandle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.17 16:54
  * Description: Модель для подавления ложных перемещений карты.
  */
object MMapDelay {

  implicit object MMapDelayFastEq extends FastEq[MMapDelay] {
    override def eqv(a: MMapDelay, b: MMapDelay): Boolean = {
      (a.timerId ==* b.timerId) &&
      (a.generation ==* b.generation) &&
      (a.reason ===* b.reason) &&
      (a.listenMove ==* b.listenMove)
    }
  }

  @inline implicit def univEq: UnivEq[MMapDelay] = UnivEq.derive

}


/** Модель данных для отложенных напотом перемещений на карте.
  *
  * @param timerId id запущенного таймера.
  * @param generation nonce для сообщений о срабатывании таймера,
  *                   чтобы гарантированно отфильтровать неактуальные события из таймеров.
  * @param listenMove Слушать ли сигналы moveend?
  *                       При кликах по ресиверам -- игнорить.
  *                       При drag -- надо слушать до победного.
  */
case class MMapDelay(
                      timerId           : SetTimeoutHandle,
                      generation        : Long,
                      reason            : MapReIndex,
                      listenMove        : Boolean
                    )
  extends UseValueEq
