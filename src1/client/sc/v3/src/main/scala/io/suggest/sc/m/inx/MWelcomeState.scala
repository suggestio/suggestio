package io.suggest.sc.m.inx

import diode.FastEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 9:56
  * Description: Состояние Welcome-карточки.
  */

object MWelcomeState {

  implicit object MWelcomeStateFastEq extends FastEq[MWelcomeState] {
    override def eqv(a: MWelcomeState, b: MWelcomeState): Boolean = {
      (a.isHiding ==* b.isHiding) &&
      (a.timerTstamp ==* b.timerTstamp)
    }
  }

  @inline implicit def univEq: UnivEq[MWelcomeState] = UnivEq.derive

}


/** Класс состояния welcome-карточки.
  *
  * @param isHiding Происходит ли сейчас сокрытие карточки?
  *                 true -- да, сейчас идёт анимация сокрытия.
  *                 false -- карточка отображается стабильно, без анимации сокрытия.
  * @param timerTstamp Таймштамп, чтобы отсеивать возможные события от уже отменённых таймеров.
  */
case class MWelcomeState(
                          isHiding         : Boolean,
                          timerTstamp      : Long,
                        )
