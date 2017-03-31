package io.suggest.lk.adv.geo.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 16:27
  * Description: Модель данных состояния попапа над георазмещениями.
  */
object MGeoAdvPopupState {

  implicit object MGeoAdvPopupStateFastEq extends FastEq[MGeoAdvPopupState] {
    override def eqv(a: MGeoAdvPopupState, b: MGeoAdvPopupState): Boolean = {
      a.open eq b.open
    }
  }

}


/**
  * Класс модели состояния попапа.
  * @param open исходный инстанс сообщения с данными для открытия попапа.
  */
case class MGeoAdvPopupState(
                              open: OpenAdvGeoExistPopup
                            )

