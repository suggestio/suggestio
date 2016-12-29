package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.geo.MGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 16:27
  * Description: Модель данных состояния попапа над георазмещениями.
  */
object MGeoAdvPopupState {

  implicit object MGeoAdvPopupStateFastEq extends FastEq[MGeoAdvPopupState] {
    override def eqv(a: MGeoAdvPopupState, b: MGeoAdvPopupState): Boolean = {
      a.position eq b.position
    }
  }

}


/**
  * Класс модели состояния попапа.
  * @param position точка втыкания попапа.
  */
case class MGeoAdvPopupState(
                              position: MGeoPoint
                            )

