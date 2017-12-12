package io.suggest.sc.m.search

import diode.UseValueEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.17 16:54
  * Description: Модель для подавления ложных перемещений карты.
  */
object MMapDelay {

  implicit def univEq: UnivEq[MMapDelay] = UnivEq.derive

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
