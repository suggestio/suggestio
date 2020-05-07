package io.suggest.sjs.dom2

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.05.2020 21:45
  * Description: Нестандартные расширения для js.Error.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error]]
  */
@js.native
trait JsErrorExt extends js.Object {

  /** Non-standard. */
  def fileName: js.UndefOr[String] = js.native

  /** Non-standard. */
  def lineNumber: js.UndefOr[Int] = js.native

  /** Non-standard. */
  def columnNumber: js.UndefOr[Int] = js.native

  /** Non-standard.
    * @see [[https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/stack]]
    */
  def stack: js.UndefOr[String] = js.native

}
