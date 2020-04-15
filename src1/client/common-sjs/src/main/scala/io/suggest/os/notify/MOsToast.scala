package io.suggest.os.notify

import io.suggest.spa.DAction
import japgolly.univeq._

import scala.scalajs.js


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.2020 12:14
  * Description: Данные одной нотификации.
  * Абстракция над cordova CnlNotification и js.dom.Notification.
  * @param onEvent Карта эффектов при наступлении указанных ключей-событий.
  * @param appBadgeCounter Значение счётчика на иконке приложения на рабочем столе или где-то ещё.
  */
final case class MOsToast(
                           uid            : String, // cnl => id, html5 => tag
                           title          : String,
                           //group          : Option[String]         = None,
                           text           : Seq[MOsToastText]      = Nil,
                           imageUrl       : Option[String]         = None, // cnl => attachments, html => image
                           smallIconUrl   : Option[String]         = None, // cnl => smallIcon, html5 => badge
                           iconUrl        : Option[String]         = None, // cnl => icon, html => icon
                           data           : Option[js.Any]         = None, // cnl => Только пригодная к JSON-сериализации инфа.
                           vibrate        : Option[Boolean]        = None,
                           silent         : Option[Boolean]        = None,
                           foreground     : Option[Boolean]        = None,
                           // action.onAction и onEvent[] явно разделены, т.к. в HTML5 listener'ы вешаются на конкретную цель.
                           actions        : Seq[MOsToastAction]    = Nil,
                           appBadgeCounter: Option[Int]            = None, // cnl => badge
                           // action handlers
                           onEvent        : Map[String, DAction]   = Map.empty,
                           sticky         : Option[Boolean]        = None
                         )
object MOsToast {
  @inline implicit def univEq: UnivEq[MOsToast] = UnivEq.force
}


/** Интерфейс для одного абстрактного экшена нотификации.
  * @see cordova-plugin-local-notification [[https://github.com/katzer/cordova-plugin-local-notifications#actions]]
  * @see HTML5 API [[https://developer.mozilla.org/en-US/docs/Web/API/NotificationAction]]
  * @param onAction Если юзер выбирает указанный экшен, то надо отправить указанный эффект на исполнение.
  *                 Возможный результат выполнения экшена передаётся в функцию сборки эффекта.
  */
final case class MOsToastAction(
                                 id              : String,
                                 title           : String,
                                 inputType       : Option[String]      = None,
                                 emptyText       : Option[String]      = None,
                                 iconUrl         : Option[String]      = None,
                                 onAction        : Option[MOsToastActionEvent => DAction]      = None,
                               )
object MOsToastAction {
  @inline implicit def univEq: UnivEq[MOsToastAction] = UnivEq.force
}


/** Данные по отображаемому тексту.
  *
  * @param text Отображаемый текст нотификации.
  * @param person cordova-only. Юзернейм для отображения слева от текста.
  */
final case class MOsToastText(
                               text              : String,
                               person            : Option[String]      = None,
                             )
object MOsToastText {
  @inline implicit def univEq: UnivEq[MOsToastText] = UnivEq.derive
}



final case class MOsToastActionEvent(
                                      text       : Option[String]     = None,
                                    )
object MOsToastActionEvent {
  @inline implicit def univEq: UnivEq[MOsToastActionEvent] = UnivEq.derive
}
