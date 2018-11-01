package chandu0101.scalajs.react.components.materialui.raw1

import japgolly.scalajs.react.ReactEventFrom
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.18 18:04
  * Description:
  */
@js.native
trait TouchTapEvent[+DOMEventTarget <: dom.Node] extends ReactEventFrom[DOMEventTarget] {
  def altKey: Boolean
  def ctrlKey: Boolean
  def getModifierState(key: String): Boolean
  def metaKey: Boolean
  def shiftKey: Boolean
}
