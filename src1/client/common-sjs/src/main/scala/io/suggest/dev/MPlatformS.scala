package io.suggest.dev

import diode.FastEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.18 15:25
  * Description: Модель данных текущей платформы для platform-контроллера.
  */
object MPlatformS {

  implicit object MPlatformSFastEq extends FastEq[MPlatformS] {
    override def eqv(a: MPlatformS, b: MPlatformS): Boolean = {
      (a.isUsingNow ==* b.isUsingNow) &&
        (a.isReady ==* b.isReady) &&
        (a.isCordova ==* b.isCordova) &&
        (a.isBleAvail ==* b.isBleAvail)
    }
  }

  implicit def univEq: UnivEq[MPlatformS] = UnivEq.derive

}


/** Контейнер данных платформы.
  *
  * @param isReady Готовность платформы к полноценной работе.
  * @param isCordova Является ли текущеая платформа cordova-окружением?
  * @param isUsingNow Активная работа с приложением сейчас?
  *                   true значит, что вкладка открыта и активна, приложение открыто и на переднем плане.
  *                   Все системы должны быть активны.
  *                   false значит, что выдача неактивна: скрыта вкладка, приложение где-то в фоне. Спать.
  * @param isBleAvail Доступен ли Bluetooth LE на данной платформе?
  *                   Следует помнить, что BLE может быть доступен, а платформа ещё не готова,
  *                   и работа с BLE API будет с не очень предсказуемыми результатами.
  */
case class MPlatformS(
                       isUsingNow     : Boolean,
                       isReady        : Boolean,
                       isCordova      : Boolean,
                       isBleAvail     : Boolean
                     ) {

  def withIsUsingNow(isUsingNow: Boolean) = copy(isUsingNow = isUsingNow)
  def withIsReady(isReady: Boolean) = copy(isReady = isReady)
  def withIsCordova(isCordova: Boolean) = copy(isCordova = isCordova)
  def withIsBleAvail(isBleAvail: Boolean) = copy(isBleAvail = isBleAvail)

}