package io.suggest.sjs.common.i18n

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 15:06
  * Description: Модель сообщения, подлежащего client-side локализации перед рендером.
  * Внутри play есть встроенная аналогичная модель.
  */
trait IMessage {

  /** Код сообщения по conf/messages.* */
  def message : String

  /** Необязательные аргументы. */
  def args    : Seq[js.Any]

}


case class MMessage(
  override val message : String,
  override val args    : js.Any*
)
  extends IMessage
