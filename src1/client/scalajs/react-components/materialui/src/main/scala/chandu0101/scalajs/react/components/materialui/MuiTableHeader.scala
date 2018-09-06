package chandu0101.scalajs.react.components
package materialui

import chandu0101.macros.tojs.JSMacro
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.`|`

/**
 * This file is generated - submit issues instead of PR against it
 */
    
case class MuiTableHeader(
  key:               js.UndefOr[String]                  = js.undefined,
  ref:               js.UndefOr[MuiTableHeaderM => Unit] = js.undefined,
  /** Controls whether or not header rows should be
     adjusted for a checkbox column. If the select all
     checkbox is true, this property will not influence
     the number of columns. This is mainly useful for
     "super header" rows so that the checkbox column
     does not create an offset that needs to be accounted
     for manually. */
  adjustForCheckbox: js.UndefOr[Boolean]                 = js.undefined,
  /** The css class name of the root element. */
  className:         js.UndefOr[String]                  = js.undefined,
  /** Controls whether or not the select all checkbox is displayed. */
  displaySelectAll:  js.UndefOr[Boolean]                 = js.undefined,
  /** If set to true, the select all button will be interactable.
     If set to false, the button will not be interactable.
     To hide the checkbox, set displaySelectAll to false. */
  enableSelectAll:   js.UndefOr[Boolean]                 = js.undefined,
  /** Callback when select all has been checked. */
  onSelectAll:       js.UndefOr[Boolean => Callback]     = js.undefined,
  /** True when select all has been checked. */
  selectAllSelected: js.UndefOr[Boolean]                 = js.undefined,
  /** Override the inline-styles of the root element. */
  style:             js.UndefOr[CssProperties]           = js.undefined){

  /**
    * @param children Children passed to table header.
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiTableHeader](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.TableHeader)
    f(props)(children: _*)
  }
}


@js.native
trait MuiTableHeaderM extends js.Object {
  def createBaseHeaderRow(): Unit = js.native

  def createSuperHeaderRow(): Unit = js.native

  def createSuperHeaderRows(): Unit = js.native

  def getCheckboxPlaceholder(): Unit = js.native

  def getSelectAllCheckboxColumn(): Unit = js.native
}
