package io.suggest.os.notify.api.html5

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.common.html.HtmlConstants._
import io.suggest.os.notify.{CloseNotify, NotificationPermAsk, NotifyStartStop, OsNotifyEvents, ShowNotify}
import org.scalajs.dom.experimental.Notification
import io.suggest.msg.ErrorMsgs
import io.suggest.perm.Html5PermissionApi
import io.suggest.primo.Keep
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sjs.dom2.NotificationOptions2
import japgolly.univeq._

import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.04.2020 11:43
  * Description: HTML5 Notification API adapter for Notify-actions.
  */
object Html5NotificationApiAdp {

  def circuitDebugInfoSafe() = (Try(Notification), Try(Notification.permission))

}


final class Html5NotificationApiAdp[M](
                                        dispatcher      : Dispatcher,
                                        modelRW         : ModelRW[M, MH5nAdpS],
                                      )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Эффект сокрытия нотификаций. */
  private def _closeToastsFx(toastIds: Iterable[String]): Effect = {
    Effect.action {
      val v0 = value

      (if ( toastIds.isEmpty ) {
        // Нет id скрываемых нотификаций - скрыть всё.
        v0.notifications
          .valuesIterator
      } else {
        toastIds
          .iterator
          .flatMap( v0.notifications.get )
      })
        .foreach { info =>
          // Скрыть нотификацию:
          try {
            info.h5Not.close()
          } catch {
            case ex: Throwable =>
              logger.warn( ErrorMsgs.INACTUAL_NOTIFICATION, ex, info.osToast )
          }
        }

      H5nRemoveNotifications( toastIds )
    }
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск/остановка нотификатора.
    case m: NotifyStartStop =>
      val v0 = value

      if (m.isStart) {
        // Сразу парсим пермишен, чтобы exception при ошибке возник как можно раньше.
        val v2 = (
          MH5nAdpS.permission set Html5NotificationUtil.readPermissionPot()
        )(v0)

        for (ex <- v2.permission.exceptionOption)
          logger.error( ErrorMsgs.NATIVE_API_ERROR, ex, msg = m )

        updated(v2)

      } else {
        // Остановка. Сброс состояния: Скрыть все нотификации.
        val fx = _closeToastsFx( Nil )
        val v2Opt = Option.when( v0.permission.nonEmpty )(
          MH5nAdpS.permission.set( Pot.empty)(v0)
        )
        ah.optionalResult( v2Opt, Some(fx) )
      }


    // Экшен отображения нотификаций.
    case m: ShowNotify =>
      if (m.toasts.nonEmpty) {
        val showNotifyFx = Effect.action {
          val addNots = (for {
            osToast <- m.toasts.iterator
          } yield {
            // Нотификация рендерится на экран прямо из конструктора нотификации.
            val h5Not = new Notification(
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
                    .get( evtType )
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
                  override val sticky = osToast.sticky.orUndefined
                  override val onclick = __eventOf( OsNotifyEvents.CLICK )
                  override val onerror = __eventOf( OsNotifyEvents.ERROR )
                  // TODO onshow, onclose - deprecated API. onshow можно запустить из конструктора.
                  override val onshow = __eventOf( OsNotifyEvents.SHOW )
                  override val onclose = __eventOf( OsNotifyEvents.CLOSE )
                }
              }
            )
            osToast.uid -> MH5nToastInfo(osToast, h5Not)
          })
            .to( HashMap )

          H5nAddNotifications( addNots )
        }

        effectOnly( showNotifyFx )

      } else {
        noChange
      }


    // Отрендерены нотификации на экран.
    case m: H5nAddNotifications =>
      val v0 = value

      val v2 = MH5nAdpS.notifications.modify(
        _.merged( m.nots )( Keep.right )
      )(v0)
      updatedSilent( v2 )


    // Сигнал к сокрытию нотификации.
    case m: CloseNotify =>
      val fx = _closeToastsFx( m.toastIds )
      effectOnly(fx)


    // Непосредственное вычищение нотификаций из состояния.
    case m: H5nRemoveNotifications =>
      val v0 = value
      val v2 = MH5nAdpS.notifications.modify(_ -- m.toastIds)(v0)
      updatedSilent( v2 )


    // Запрос текущего состояния разрешения на вывод уведомлений.
    case m: NotificationPermAsk =>
      lazy val permResFut = if (m.isVisible) {
        JsApiUtil
          .call1Fut( Notification.requestPermission )
          .map( Html5PermissionApi.parsePermissionValue )
      } else {
        val parsed = Html5PermissionApi.parsePermissionValue( Notification.permission )
        Future.successful( parsed )
      }

      var fx: Effect = Effect {
        permResFut.map( H5nSavePermFx )
      }

      for (onComplete <- m.onComplete) {
        val replySenderFx = Effect {
          permResFut.map( onComplete )
        }
        val fx0 = fx
        fx = replySenderFx >> fx0
      }

      // Выставить pending в состояние пермишшена.
      val v0 = value
      val v2Opt = if (v0.permission.isPending) {
        None
      } else {
        val v2 = MH5nAdpS.permission.modify( _.pending() )(v0)
        Some( v2 )
      }

      ah.optionalResult( v2Opt, Some(fx) )


    // Сохранить пермишшен в состоянии.
    case m: H5nSavePermFx =>
      val v0 = value

      val v2 = MH5nAdpS.permission.modify { permission0 =>
        m.perm.fold( permission0.unPending )( permission0.ready )
      }(v0)

      updated( v2 )

  }

}
