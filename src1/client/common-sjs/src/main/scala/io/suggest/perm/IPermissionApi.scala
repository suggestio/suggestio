package io.suggest.perm

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 19:06
  * Description: Интерфейс для получения инфы по одному пермишшену.
  * Для проверок разных прав используются разные реализации одного интерфейса.
  *
  * - Доступность геолокации можно быстро проверить:
  *   - HTML5 Permissions API (Не все браузеры умеют API + не все браузеры с API умеют name=geolocation).
  *   - cordova-diagnostic (android)
  */
trait IPermissionApi {

  /** Получение инфы по состоянию доступа.
    *
    * @return [[IPermissionState]], если API поддерживается и отвечает.
    * @throws NoSuchElementException когда API недоступно.
    */
  def getPermissionState(): Future[IPermissionState]

}


/** Результат проверки одного пермишена. */
trait IPermissionState {

  /** Если элемент допускает управление питанием, то текущее состояние питания.
    * Если питание неактуально, то всегда true. */
  def isPoweredOn: Boolean

  /** Доступ разрешён. */
  def isGranted: Boolean

  /** В доступе отказано. */
  def isDenied: Boolean

  /** Можно ли подписаться на измение состояния? */
  def hasOnChangeApi: Boolean

  /** Включить подписку на изменение состояния. */
  def onChange(f: Boolean => _): Unit

  /** Выключить мониторинг изменения состояния. */
  def onChangeReset(): Unit

}
