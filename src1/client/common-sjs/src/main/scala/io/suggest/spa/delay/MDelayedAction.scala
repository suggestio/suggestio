package io.suggest.spa.delay

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 16:20
  * Description: Модель данных по одному отложенному сообщению.
  */
object MDelayedAction {

  implicit def univEq: UnivEq[MDelayedAction] = UnivEq.derive

}


/** Класс модели данных по одному отложенному ws-сообщению.
  *
  * @param info Исходные данные по отложенному во времени экшену.
  */
case class MDelayedAction(
                           info      : DelayAction
                         )
