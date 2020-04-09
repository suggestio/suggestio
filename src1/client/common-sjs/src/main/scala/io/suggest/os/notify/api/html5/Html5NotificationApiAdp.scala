package io.suggest.os.notify.api.html5

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.common.html.HtmlConstants._
import io.suggest.os.notify.{CloseNotify, NotifyPermission, NotifyStartStop, OsNotifyEvents, ShowNotify}
import org.scalajs.dom.experimental.Notification
import io.suggest.msg.ErrorMsgs
import io.suggest.perm.Html5PermissionApi
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom2.NotificationOptions2
import io.suggest.spa.{DAction, DoNothing}
import japgolly.univeq._

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.04.2020 11:43
  * Description: HTML5 Notification API adapter for Notify-actions.
  */
final class Html5NotificationApiAdp[M](
                                        dispatcher      : Dispatcher,
                                        modelRW         : ModelRW[M, Option[MHtml5NotifyAdpS]],
                                      )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск/остановка нотификатора.
    case m: NotifyStartStop =>
      value.fold {
        if (m.isStart) {
          val tryRes = Try {
            val v2Opt = Some( MHtml5NotifyAdpS(
              permission = Html5PermissionApi.parsePermissionValue( Notification.permission ),
            ))
            updatedSilent(v2Opt)
          }
          for (ex <- tryRes.failed)
            LOG.error( ErrorMsgs.NATIVE_API_ERROR, ex, msg = m )

          tryRes.getOrElse( noChange )

        } else {
          noChange
        }
      } { _ =>
        // Скрыть все нотификации.
        if (m.isStart) {
          noChange
        } else {
          updatedSilent( None )
        }
      }


    // Экшен отображения нотификаций.
    case m: ShowNotify =>
      (for {
        _ <- value
        if m.toasts.nonEmpty
      } yield {
        val showNotifyFx = Effect.action {
          for (osToast <- m.toasts) {
            new Notification(
              title = osToast.title,
              options = {
                val _body: String = osToast
                  .text
                  .iterator
                  .map { osNotifyText =>
                    osNotifyText
                      .person
                      .fold( osNotifyText.text )( _ + SPACE + osNotifyText.text )
                  }
                  .mkString( NEWLINE_UNIX.toString )

                /** Извлечение поддерживаемых событий из карты событий. */
                def __eventOf(evtType: String) = {
                  osToast
                    .onEvent
                    .get( OsNotifyEvents.CLICK )
                    .map[js.Function0[Any]] { dAction =>
                      () =>
                        dispatcher.dispatch( dAction )
                    }
                    .toUndef
                }

                new NotificationOptions2 {
                  override val body = _body
                  override val icon = osToast.iconUrl.toUndef
                  override val silent = osToast.silent.toUndef
                  override val image = osToast.imageUrl.toUndef
                  override val badge = osToast.smallIconUrl.toUndef
                  override val vibrate = {
                    if (osToast.vibrate contains[Boolean] true)
                      js.Array[Double](200, 100, 200)
                    else
                      js.undefined
                  }
                  override val tag = osToast.uid
                  override val onclick = __eventOf( OsNotifyEvents.CLICK )
                  override val onerror = __eventOf( OsNotifyEvents.ERROR )
                  // TODO onshow, onclose - deprecated API. onshow можно запустить из конструктора.
                  override val onshow = __eventOf( OsNotifyEvents.SHOW )
                  override val onclose = __eventOf( OsNotifyEvents.CLOSE )
                }
              }
            )
          }

          DoNothing
        }

        effectOnly( showNotifyFx )
      })
        .getOrElse( noChange )


    // Сигнал к сокрытию нотификации.
    case m: CloseNotify =>
      // У нас инстансы НЕ хранятся в состоянии. Чтобы скрыть нотификейшен, надо этот инстанс создать.
      // Создать нотификейшен с указанным tag и скрыть его.
      val fx = Effect.action {
        for (toadId <- m.toastIds) {
          new Notification(
            title = "",
            options = new NotificationOptions2 {
              override val tag = toadId
            }
          )
            .close()
        }

        DoNothing
      }
      effectOnly(fx)


    // Запрос текущего состояния разрешения на вывод уведомлений.
    case m: NotifyPermission =>
      lazy val permResFut = if (m.isRequest) {
        JsApiUtil
          .call1Fut( Notification.requestPermission )
          .map( Html5PermissionApi.parsePermissionValue )
      } else {
        val parsed = Html5PermissionApi.parsePermissionValue( Notification.permission )
        Future.successful( parsed )
      }


      var fx: Effect = Effect {
        permResFut.map( H5NaSaveFx )
      }

      for (onComplete <- m.onComplete) {
        val replySenderFx = Effect {
          permResFut.map( onComplete )
        }
        val fx0 = fx
        fx = replySenderFx >> fx0
      }

      effectOnly( fx )


    // Сохранить пермишшен в состоянии.
    case m: H5NaSaveFx =>
      value.fold(noChange) { v0 =>
        if (v0.permission ==* m.perm) {
          noChange
        } else {
          val v2 = (MHtml5NotifyAdpS.permission set m.perm)(v0)
          updatedSilent( Some(v2) )
        }
      }

  }

}


/** Закэшировать результат проверки в состоянии. */
private case class H5NaSaveFx( perm: Option[Boolean] ) extends DAction
