package io.suggest.lk.vm

import io.suggest.i18n.I18nConstants
import io.suggest.sjs.common.i18n.{JsMessager, JsMessagesSingleLang}
import org.scalajs.dom
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.language.implicitConversions
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:18
  * Description: Доступ к полю window._SioLkMessages с кодами локализаций.
  */
@js.native
trait LkMessagesWindow extends js.Object {

  @JSName( I18nConstants.LK_MESSAGES_JSNAME )
  val lkJsMessages: JsMessagesSingleLang = js.native

}


object LkMessagesWindow extends JsMessager {

  override val Messages = fromWindow().lkJsMessages

  implicit def fromWindow(window: Window = dom.window): LkMessagesWindow = {
    window.asInstanceOf[LkMessagesWindow]
  }

}
