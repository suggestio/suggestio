package io.suggest.i18n

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 15:06
  * Description: Модель сообщения, подлежащего client-side локализации перед рендером.
  * Внутри play есть встроенная похожая модель.
  *
  * 2016.dec.19: Из-за интеграции с boopickle пришлось пока отказаться от args: Any* в пользу явного Seq[Int].
  */
trait IMessage {

  /** Код сообщения по conf/messages.* */
  def message : String

  /** Необязательные аргументы для рендера сообщения. */
  // TODO Было тут изначально js.Any, потом Any, потом оказалось, что boopickle не умеет в Any.
  def args    : Seq[Int]

}


object MMessage {

  import boopickle.Default._

  implicit val picker: Pickler[MMessage] = generatePickler[MMessage]

  /** Это такое apply, только с другим именем, т.к. конфликтует с apply(String, Seq) от case-класса. */
  def a(message: String, args: Int*): MMessage = {
    MMessage(message, args)
  }

}



/** Инстанс модели одного нелокализованного параметризованного сообщения об ошибке. */
case class MMessage(
  override val message : String,
  // Раньше было Any*, но boopickle не умеет ни Any, ни *. Поэтому всё по упрощенке:
  override val args    : Seq[Int] = Nil
)
  extends IMessage
