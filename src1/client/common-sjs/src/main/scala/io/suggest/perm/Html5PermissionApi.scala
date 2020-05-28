package io.suggest.perm

import io.suggest.common.empty.OptionUtil
import io.suggest.event.DomEvents
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import org.scalajs.dom.experimental.permissions._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import org.scalajs.dom
import org.scalajs.dom.Event
import OptionUtil.BoolOptOps
import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 19:13
  * Description: Доступ к Permissions API.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/API/Permissions_API]]
  */
object Html5PermissionApi extends Log {

  /** Безопасная проверка наличия HTML5 Permissions API в текущем User-Agent. */
  def isApiAvail(): Boolean = {
    val tryResOpt = Try {
      import Html5PermissionApiStub._
      for {
        wNav <- WindowVm().navigator
        wnPerm <- wNav.permissions
        if JsApiUtil.isDefinedSafe( wnPerm.queryU )
      } yield {
        true
      }
    }

    for (ex <- tryResOpt.failed)
      logger.error( ErrorMsgs.PERMISSION_API_FAILED, ex )

    tryResOpt
      .toOption
      .flatten
      .getOrElseFalse
  }

  /** Доступ к данным разрешения.
    *
    * @param permName id пермишшена.
    * @return Фьючерс с результатом.
    */
  def getPermissionState(permName: PermissionName ): Future[IPermissionState] = {
    try {
      dom.window.navigator
        // TODO Проверять, доступен ли HTML5 Permissions API вообще. В iOS Safari - его нет.
        .permissions
        .query {
          new PermissionDescriptor {
            override val name = permName
          }
        }
        .toFuture
        .map( Html5PermissionState.apply )
    } catch {
      case ex: Throwable =>
        Future.failed( new UnsupportedOperationException(ex) )
    }
  }


  /** Дополнительное API для инстансов HTML5 PermissionState. */
  implicit final class PermissionStatusExt( private val pState: PermissionState ) extends AnyVal {

    def isGranted: Boolean =
      pState ==* PermissionState.granted

    def isDenied: Boolean =
      pState ==* PermissionState.denied

    def isPrompt: Boolean =
      pState ==* PermissionState.prompt

  }


  /** Парсить значение пермишшена. */
  def parsePermissionValue(permissionValue: String): Option[Boolean] = {
    if (permissionValue ==* PermissionState.granted.asInstanceOf[String])
      OptionUtil.SomeBool.someTrue
    else if (permissionValue ==* PermissionState.denied.asInstanceOf[String] )
      OptionUtil.SomeBool.someFalse
    else /* default для HTML5 Notification API || prompt для HTML5 Permission API */
      None
  }

}


/** Унифицированная обёртка над [[IPermissionState]]. */
case class Html5PermissionState(
                                 pStatus: PermissionStatus
                               )
  extends IPermissionState
{

  import Html5PermissionApi._

  /** Управление питанием GPS не затрагивается в html5-спеках. */
  override def isPoweredOn = true

  override def isGranted: Boolean =
    pStatus.state.isGranted

  override def isDenied: Boolean =
    pStatus.state.isDenied

  override def isPrompt: Boolean =
    pStatus.state.isPrompt

  override def hasOnChangeApi = true

  override def onChange(f: IPermissionState => _): Unit = {
    // 2018-01-25 Кусок кода *временно* оставлен как замена addEventListener, т.к. в файрфоксе 64-dev не работал ни один из кусков кода. TODO Удалить, когда всё прояснится-отладится.
    /*
    pStatus.onchange = ({
      (thisStatus: PermissionStatus, _: Event) =>
        println(thisStatus, 1)
        f( this.snapshot(thisStatus.state.isGranted) )
        println(thisStatus, 2)
    }: js.ThisFunction1[PermissionStatus, Event, _])
        .asInstanceOf[js.Function1[Event, _]]
    */
    pStatus.addEventListenerThis( DomEvents.CHANGE ) {
      (thisStatus: PermissionStatus, _: Event) =>
        f( this.snapshot(thisStatus.state.isGranted) )
    }
  }

  /** Выключить мониторинг изменения состояния. */
  override def onChangeReset(): Unit =
    pStatus.onchange = null

}
