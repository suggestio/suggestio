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
    
case class MuiSelectField[T](
  key:                    js.UndefOr[String]                              = js.undefined,
  ref:                    js.UndefOr[String]                              = js.undefined,
  /** If true, the width will automatically be set according to the
     items inside the menu.
     To control the width in CSS instead, leave this prop set to `false`. */
  autoWidth:              js.UndefOr[Boolean]                             = js.undefined,
  /** If true, the select field will be disabled. */
  disabled:               js.UndefOr[Boolean]                             = js.undefined,
  /** Object that can handle and override any property of component DropDownMenu. */
  dropDownMenuProps:      js.UndefOr[DropDownMenuProps]                   = js.undefined,
  /** Override the inline-styles of the error element. */
  errorStyle:             js.UndefOr[CssProperties]                       = js.undefined,
  /** The error content to display. */
  errorText:              js.UndefOr[VdomNode]                            = js.undefined,
  /** If true, the floating label will float even when no value is selected. */
  floatingLabelFixed:     js.UndefOr[Boolean]                             = js.undefined,
  /** Override the inline-styles of the floating label. */
  floatingLabelStyle:     js.UndefOr[CssProperties]                       = js.undefined,
  /** The content of the floating label. */
  floatingLabelText:      js.UndefOr[VdomNode]                            = js.undefined,
  /** If true, the select field will take up the full width of its container. */
  fullWidth:              js.UndefOr[Boolean]                             = js.undefined,
  /** Override the inline-styles of the hint element. */
  hintStyle:              js.UndefOr[CssProperties]                       = js.undefined,
  /** The hint content to display. */
  hintText:               js.UndefOr[VdomNode]                            = js.undefined,
  /** Override the inline-styles of the icon element. */
  iconStyle:              js.UndefOr[CssProperties]                       = js.undefined,
  /** The id prop for the text field. */
  id:                     js.UndefOr[String]                              = js.undefined,
  /** Override the label style when the select field is inactive. */
  labelStyle:             js.UndefOr[CssProperties]                       = js.undefined,
  /** Override the inline-styles of the underlying `List` element. */
  listStyle:              js.UndefOr[CssProperties]                       = js.undefined,
  /** Override the default max-height of the underlying `DropDownMenu` element. */
  maxHeight:              js.UndefOr[Double]                              = js.undefined,
  /** Override the inline-styles of menu items. */
  menuItemStyle:          js.UndefOr[CssProperties]                       = js.undefined,
  /** Override the inline-styles of the underlying `DropDownMenu` element. */
  menuStyle:              js.UndefOr[CssProperties]                       = js.undefined,
  /** If true, `value` must be an array and the menu will support
     multiple selections. */
  multiple:               js.UndefOr[Boolean]                             = js.undefined,
  onBlur:                 js.UndefOr[ReactFocusEvent => Callback]         = js.undefined,
  /** Callback function fired when a menu item is selected.
     @param {object} event Click event targeting the menu item
     that was selected.
     @param {number} key The index of the selected menu item, or undefined
     if `multiple` is true.
     @param {any} payload If `multiple` is true, the menu's `value`
     array with either the menu item's `value` added (if
     it wasn't already selected) or omitted (if it was already selected).
     Otherwise, the `value` of the menu item. */
  onChange:               js.UndefOr[(TouchTapEvent, Int, T) => Callback] = js.undefined,
  onFocus:                js.UndefOr[ReactFocusEvent => Callback]         = js.undefined,
  /** Override the inline-styles of selected menu items. */
  selectedMenuItemStyle:  js.UndefOr[CssProperties]                       = js.undefined,
  /** Customize the rendering of the selected item.
     @param {any} value If `multiple` is true, the menu's `value`
     array with either the menu item's `value` added (if
     it wasn't already selected) or omitted (if it was already selected).
     Otherwise, the `value` of the menu item.
     @param {any} menuItem The selected `MenuItem`.
     If `multiple` is true, this will be an array with the `MenuItem`s matching the `value`s parameter. */
  selectionRenderer:      js.UndefOr[Callback]                            = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                  js.UndefOr[CssProperties]                       = js.undefined,
  /** Override the inline-styles of the underline element when the select
     field is disabled. */
  underlineDisabledStyle: js.UndefOr[CssProperties]                       = js.undefined,
  /** Override the inline-styles of the underline element when the select field
     is focused. */
  underlineFocusStyle:    js.UndefOr[CssProperties]                       = js.undefined,
  /** Override the inline-styles of the underline element. */
  underlineStyle:         js.UndefOr[CssProperties]                       = js.undefined,
  /** If `multiple` is true, an array of the `value`s of the selected
     menu items. Otherwise, the `value` of the selected menu item.
     If provided, the menu will be a controlled component. */
  value:                  js.UndefOr[T]                                   = js.undefined,
  /** This is the point on the anchor that the popover's
     `targetOrigin` will attach to.
     Options:
     vertical: [top, center, bottom]
     horizontal: [left, middle, right].
     (Passed on to DropDownMenu) */
  anchorOrigin:           js.UndefOr[Origin]                              = js.undefined,
  /** If true, the popover will apply transitions when
     it gets added to the DOM.
     (Passed on to DropDownMenu) */
  animated:               js.UndefOr[Boolean]                             = js.undefined,
  /** Override the default animation component used.
     (Passed on to DropDownMenu) */
  animation:              js.UndefOr[js.Function]                         = js.undefined,
  /** The css class name of the root element.
     (Passed on to DropDownMenu) */
  className:              js.UndefOr[String]                              = js.undefined,
  /** Overrides default `SvgIcon` dropdown arrow component.
     (Passed on to DropDownMenu) */
  iconButton:             js.UndefOr[VdomNode]                            = js.undefined,
  /** Callback function fired when the menu is closed.
     (Passed on to DropDownMenu) */
  onClose:                js.UndefOr[Callback]                            = js.undefined,
  /** Set to true to have the `DropDownMenu` automatically open on mount.
     (Passed on to DropDownMenu) */
  openImmediately:        js.UndefOr[Boolean]                             = js.undefined,
  /** This is the point on the popover which will attach to
     the anchor's origin.
     Options:
     vertical: [top, center, bottom]
     horizontal: [left, middle, right].
     (Passed on to DropDownMenu) */
  targetOrigin:           js.UndefOr[Origin]                              = js.undefined){

  /**
    * @param children The `MenuItem` elements to populate the select field with.
    If the menu items have a `label` prop, that value will
    represent the selected menu item in the rendered select field.
   */
  def apply(children: VdomNode*) = {
    implicit def evT(t: T): js.Any = t.asInstanceOf[js.Any]
implicit def ev2T(t: T | js.Array[T]): js.Any = t.asInstanceOf[js.Any]
    val props = JSMacro[MuiSelectField[T]](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.SelectField)
    f(props)(children: _*)
  }
}
