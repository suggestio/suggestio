package cordova.plugins.notification.local

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.2020 7:29
  * Description: Метаданные передаются в любую callback-функцию вторым или первым (cancelall, clearall) аргументом.
  * Данные генерятся на стороне cordova, но для возможного ручного вызова fireEvent() тут sjsDefined trait.
  */
trait CnlEventData extends js.Object {

  // Общие поля для fireEvent() любого события.
  val event: String
  val foreground: Boolean
  val queued: Boolean

  /** Целочисленный id нотификейшена.
    * Для clearall/cancelall - всегда undefined.
    */
  val notification: js.UndefOr[Int] = js.undefined

  /** Поле text содержит значение action input'а внутри нотификейшена.
    * @see android/ClickReceiver.java метод setTextInput()
    */
  val text: js.UndefOr[String] = js.undefined

}
