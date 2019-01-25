package io.suggest.perm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 19:06
  * Description: Интерфейс для получения инфы по одному абстрактному пермишшену.
  * Для проверок разных прав используются разные реализации одного интерфейса.
  *
  * Например, доступность геолокации можно быстро проверить:
  * - HTML5 Permissions API (Не все браузеры умеют API + не все браузеры с API умеют name=geolocation).
  * - cordova-diagnostic (android)
  */
trait IPermissionState {

  /** Если объект доступа допускает управление питанием, то текущее состояние питания.
    * Если питание неактуально, то всегда true. */
  def isPoweredOn: Boolean

  /** Доступ разрешён. */
  def isGranted: Boolean

  /** В доступе отказано. */
  def isDenied: Boolean

  /** Статус неопределён, т.к. разрешение не запрашивалось. */
  def isPrompt: Boolean

  /** Можно ли подписаться на измение состояния? */
  def hasOnChangeApi: Boolean

  /** Включить подписку на изменение состояния. */
  def onChange(f: IPermissionState => _): Unit

  /** Выключить мониторинг изменения состояния. */
  def onChangeReset(): Unit

}

object IPermissionState {
  implicit class IpsOpsExt( val state: IPermissionState ) extends AnyVal {
    def snapshot(isGranted: Boolean): IPermissionState =
      PermissionStateSnapshot( isGranted, state )
  }
}


/** Реализация [[IPermissionState]] с фиксированным значением.
  * Появился для унифицированной onChange-реакции.
  */
case class PermissionStateSnapshot(
                                    override val isGranted: Boolean,
                                    parent: IPermissionState
                                  )
  extends IPermissionState
{
  override def isPoweredOn = parent.isPoweredOn
  override def isDenied = !isGranted
  override def isPrompt = false
  override def hasOnChangeApi = parent.hasOnChangeApi
  override def onChange(f: IPermissionState => _) = parent.onChange(f)
  override def onChangeReset() = parent.onChangeReset()
}
