package io.suggest.sjs.dom2

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.05.2020 18:36
  * Description: Расширенная поддержка для https://developer.mozilla.org/en-US/docs/Web/API/ErrorEvent
  */
@js.native
trait DomErrorEventExt extends js.Object {

  val error: js.UndefOr[js.Error] = js.native

  @JSName("colno")
  def colnoU: js.UndefOr[Int] = js.native

  @JSName("filename")
  def filenameU: js.UndefOr[String] = js.native

  @JSName("lineno")
  def linenoU: js.UndefOr[Int] = js.native

  @JSName("message")
  def messageU: js.UndefOr[String] = js.native

}
