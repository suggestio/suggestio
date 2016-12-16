package io.suggest.i18n

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
  def args    : Seq[Any]

}


case class MMessage(
  override val message : String,
  override val args    : Any*
)
  extends IMessage
