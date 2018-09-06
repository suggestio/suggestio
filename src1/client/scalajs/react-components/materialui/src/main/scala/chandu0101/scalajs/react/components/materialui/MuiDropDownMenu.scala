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
    
case class MuiDropDownMenu[T](
  key:                   js.UndefOr[String]                              = js.undefined,
  ref:                   js.UndefOr[MuiDropDownMenuM => Unit]            = js.undefined,
  /** This is the point on the anchor that the popover's
     `targetOrigin` will attach to.
     Options:
     vertical: [top, center, bottom]
     horizontal: [left, middle, right]. */
  anchorOrigin:          js.UndefOr[Origin]                              = js.undefined,
  /** If true, the popover will apply transitions when
     it gets added to the DOM. */
  animated:              js.UndefOr[Boolean]                             = js.undefined,
  /** Override the default animation component used. */
  animation:             js.UndefOr[js.Function]                         = js.undefined,
  /** The width will automatically be set according to the items inside the menu.
     To control this width in css instead, set this prop to `false`. */
  autoWidth:             js.UndefOr[Boolean]                             = js.undefined,
  /** The css class name of the root element. */
  className:             js.UndefOr[String]                              = js.undefined,
  /** Disables the menu. */
  disabled:              js.UndefOr[Boolean]                             = js.undefined,
  /** Overrides default `SvgIcon` dropdown arrow component. */
  iconButton:            js.UndefOr[VdomNode]                            = js.undefined,
  /** Overrides the styles of icon element. */
  iconStyle:             js.UndefOr[CssProperties]                       = js.undefined,
  /** Overrides the styles of label when the `DropDownMenu` is inactive. */
  labelStyle:            js.UndefOr[CssProperties]                       = js.undefined,
  /** The style object to use to override underlying list style. */
  listStyle:             js.UndefOr[CssProperties]                       = js.undefined,
  /** The maximum height of the `Menu` when it is displayed. */
  maxHeight:             js.UndefOr[Double]                              = js.undefined,
  /** Override the inline-styles of menu items. */
  menuItemStyle:         js.UndefOr[CssProperties]                       = js.undefined,
  /** Overrides the styles of `Menu` when the `DropDownMenu` is displayed. */
  menuStyle:             js.UndefOr[CssProperties]                       = js.undefined,
  /** If true, `value` must be an array and the menu will support
     multiple selections. */
  multiple:              js.UndefOr[Boolean]                             = js.undefined,
  /** Callback function fired when a menu item is clicked, other than the one currently selected.
     @param {object} event Click event targeting the menu item that was clicked.
     @param {number} key The index of the clicked menu item in the `children` collection.
     @param {any} value If `multiple` is true, the menu's `value`
     array with either the menu item's `value` added (if
     it wasn't already selected) or omitted (if it was already selected).
     Otherwise, the `value` of the menu item. */
  onChange:              js.UndefOr[(TouchTapEvent, Int, T) => Callback] = js.undefined,
  /** Callback function fired when the menu is closed. */
  onClose:               js.UndefOr[Callback]                            = js.undefined,
  /** Set to true to have the `DropDownMenu` automatically open on mount. */
  openImmediately:       js.UndefOr[Boolean]                             = js.undefined,
  /** Override the inline-styles of selected menu items. */
  selectedMenuItemStyle: js.UndefOr[CssProperties]                       = js.undefined,
  /** Callback function fired when a menu item is clicked, other than the one currently selected.
     @param {any} value If `multiple` is true, the menu's `value`
     array with either the menu item's `value` added (if
     it wasn't already selected) or omitted (if it was already selected).
     Otherwise, the `value` of the menu item.
     @param {any} menuItem The selected `MenuItem`.
     If `multiple` is true, this will be an array with the `MenuItem`s matching the `value`s parameter. */
  selectionRenderer:     js.UndefOr[Callback]                            = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                 js.UndefOr[CssProperties]                       = js.undefined,
  /** This is the point on the popover which will attach to
     the anchor's origin.
     Options:
     vertical: [top, center, bottom]
     horizontal: [left, middle, right]. */
  targetOrigin:          js.UndefOr[Origin]                              = js.undefined,
  /** Overrides the inline-styles of the underline. */
  underlineStyle:        js.UndefOr[CssProperties]                       = js.undefined,
  /** If `multiple` is true, an array of the `value`s of the selected
     menu items. Otherwise, the `value` of the selected menu item.
     If provided, the menu will be a controlled component. */
  value:                 js.UndefOr[T]                                   = js.undefined){

  /**
    * @param children The `MenuItem`s to populate the `Menu` with. If the `MenuItems` have the
    prop `label` that value will be used to render the representation of that
    item within the field.
   */
  def apply(children: VdomNode*) = {
    implicit def evT(t: T): js.Any = t.asInstanceOf[js.Any]
implicit def ev2T(t: T | js.Array[T]): js.Any = t.asInstanceOf[js.Any]
    val props = JSMacro[MuiDropDownMenu[T]](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.DropDownMenu)
    f(props)(children: _*)
  }
}


@js.native
trait MuiDropDownMenuM extends js.Object {
  def getInputNode(): js.Any = js.native

  def setWidth(): Unit = js.native
}
