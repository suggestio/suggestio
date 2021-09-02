package io.suggest.perm

import diode.data.Pot
import org.scalajs.dom.experimental.permissions.Permissions

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

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

  override final def toString: String = {
    val sb = new StringBuilder( 32, getClass.getSimpleName )
      .append( '(' )

    if (isGranted)
      sb.append('+')
    if (isDenied)
      sb.append('-')
    if (isPrompt)
      sb.append('?')
    if (isPoweredOn)
      sb.append('^')
    if (hasOnChangeApi)
      sb.append('~')

    sb.append( ')' )
      .toString()
  }

}

object IPermissionState {

  implicit final class IpsOpsExt( private val state: IPermissionState ) extends AnyVal {
    def snapshot(isGranted: Boolean): IPermissionState =
      PermissionStateSnapshot( isGranted, state )
  }

  /** Неопределённый результат работы. */
  def unknown: IPermissionState = BoolOptPermissionState( None )

  def maybeKnownF(apiAvailable: Boolean)(doApiCall: => Future[IPermissionState]): Future[IPermissionState] = {
    if (apiAvailable) doApiCall
    else Future.successful( unknown )
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


/** Реализация [[IPermissionState]] поверх опционального true-false. */
final case class BoolOptPermissionState(
                                         permitted: Option[Boolean],
                                       )
  extends IPermissionState
{
  override def isPoweredOn = true
  override def isGranted = permitted.contains(true)
  override def isDenied = permitted.contains(false)
  override def isPrompt = permitted.isEmpty

  /** Можно ли подписаться на измение состояния? */
  override def hasOnChangeApi = false
  override def onChange(f: Function[IPermissionState, _]): Unit = ???
  override def onChangeReset(): Unit = ???
}


@js.native
trait Html5PermissionApiStub extends js.Object {
  @JSName("query")
  def queryU: js.UndefOr[js.Function] = js.native
}
object Html5PermissionApiStub {
  implicit def h5stub( domPermissions: Permissions ): Html5PermissionApiStub =
    domPermissions.asInstanceOf[Html5PermissionApiStub]
}
