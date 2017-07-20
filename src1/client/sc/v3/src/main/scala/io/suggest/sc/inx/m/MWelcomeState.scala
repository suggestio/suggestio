package io.suggest.sc.inx.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 9:56
  * Description: Состояние Welcome-карточки.
  */

object MWelcomeState {

  implicit object MWelcomeStateFastEq extends FastEq[MWelcomeState] {
    override def eqv(a: MWelcomeState, b: MWelcomeState): Boolean = {
      (a.isHiding == b.isHiding) &&
        (a.timerTstamp == b.timerTstamp)
    }
  }

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
                          timerTstamp      : Long
                        )
