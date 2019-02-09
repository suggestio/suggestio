package io.suggest.sc.m.search

import diode.{FastEq, UseValueEq}
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

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
      (a.rcvrId ===* b.rcvrId) &&
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
  * @param rcvrId Выставить указанный текущий узел в состояние.
  * @param listenMove Слушать ли сигналы moveend?
  *                       При кликах по ресиверам -- игнорить.
  *                       При drag -- надо слушать до победного.
  */
case class MMapDelay(
                      timerId           : Int,
                      generation        : Long,
                      rcvrId            : Option[String],
                      listenMove        : Boolean
                    )
  extends UseValueEq
