package com.materialui

import scala.scalajs.js
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.18 21:23
  */
trait MuiPropsBase extends js.Object {
  val onClick:                     js.UndefOr[js.Function1[ReactEvent, Unit]]                     = js.undefined
  val onKeyPress:                  js.UndefOr[js.Function1[ReactKeyboardEventFromInput, Unit]]    = js.undefined
  val onMouseDown:                 js.UndefOr[js.Function1[ReactMouseEventFromInput, Unit]]       = js.undefined
  /** Unit function fired when the `ListItem` is focused or blurred by the keyboard.
     @param {object} event `focus` or `blur` event targeting the `ListItem`.
     @param {boolean} isKeyboardFocused If true, the `ListItem` is focused. */
  val onKeyboardFocus:             js.UndefOr[js.Function2[ReactFocusEvent, Boolean, Unit]]       = js.undefined
  val onMouseEnter:                js.UndefOr[js.Function1[ReactMouseEvent, Unit]]                = js.undefined
  val onMouseLeave:                js.UndefOr[js.Function1[ReactMouseEvent, Unit]]                = js.undefined
  val onMouseMove:                 js.UndefOr[js.Function1[ReactMouseEventFromInput, Unit]]       = js.undefined
  val onMouseUp:                   js.UndefOr[js.Function1[ReactMouseEventFromInput, Unit]]       = js.undefined
  val onPaste:                     js.UndefOr[js.Function1[ReactClipboardEventFromInput, Unit]]   = js.undefined
  /** Unit function fired when the `ListItem` toggles its nested list.
     @param {object} listItem The `ListItem`. */
  val onNestedListToggle:          js.UndefOr[js.Function1[js.Any, Unit]]                         = js.undefined
  val onBlur:                      js.UndefOr[js.Function1[ReactFocusEvent, Unit]]                = js.undefined
  val onFocus:                     js.UndefOr[js.Function1[ReactFocusEvent, Unit]]                = js.undefined
  val onKeyDown:                   js.UndefOr[js.Function1[ReactKeyboardEvent, Unit]]             = js.undefined
  val onKeyUp:                     js.UndefOr[js.Function1[ReactKeyboardEvent, Unit]]             = js.undefined
  val onScroll:                    js.UndefOr[js.Function1[ReactUIEventFromHtml, Unit]]           = js.undefined
  val onSelect:                 js.UndefOr[js.Function1[ReactUIEventFromInput, Unit]]             = js.undefined
  val onSubmit:                 js.UndefOr[js.Function1[ReactEventFromInput , Unit]]              = js.undefined
  val onTouchEnd:               js.UndefOr[js.Function1[ReactTouchEvent, Unit]]                   = js.undefined
  val onTouchStart:             js.UndefOr[js.Function1[ReactTouchEvent, Unit]]                   = js.undefined
  val onTouchCancel:            js.UndefOr[js.Function1[ReactTouchEventFromInput, Unit]]          = js.undefined
  val onTouchMove:              js.UndefOr[js.Function1[ReactTouchEventFromInput, Unit]]          = js.undefined
  val onTransitionEnd:          js.UndefOr[js.Function1[ReactTouchEventFromInput, Unit]]          = js.undefined
  val onWheel:                  js.UndefOr[js.Function1[ReactWheelEventFromInput, Unit]]          = js.undefined

  val onAnimationEnd:           js.UndefOr[js.Function1[ReactEventFromInput, Unit]]               = js.undefined
  val onAnimationIteration:     js.UndefOr[js.Function1[ReactEventFromInput, Unit]]               = js.undefined
  val onAnimationStart:         js.UndefOr[js.Function1[ReactEventFromInput, Unit]]               = js.undefined
  val onCompositionEnd:         js.UndefOr[js.Function1[ReactCompositionEventFromInput, Unit]]    = js.undefined
  val onCompositionStart:       js.UndefOr[js.Function1[ReactCompositionEventFromInput, Unit]]    = js.undefined
  val onCompositionUpdate:      js.UndefOr[js.Function1[ReactCompositionEventFromInput, Unit]]    = js.undefined
  val onContextMenu:            js.UndefOr[js.Function1[ReactEventFromInput, Unit]]               = js.undefined
  val onCopy:                   js.UndefOr[js.Function1[ReactClipboardEventFromInput, Unit]]      = js.undefined
  val onCut:                    js.UndefOr[js.Function1[ReactClipboardEventFromInput, Unit]]      = js.undefined
  val onDoubleClick:            js.UndefOr[js.Function1[ReactMouseEventFromInput, Unit]]          = js.undefined
  val onDrag:                   js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDragEnd:                js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDragEnter:              js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDragExit:               js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDragLeave:              js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDragOver:               js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDragStart:              js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined
  val onDrop:                   js.UndefOr[js.Function1[ReactDragEventFromInput, Unit]]           = js.undefined

  val onInput:                  js.UndefOr[js.Function1[ReactKeyboardEventFromInput, Unit]]       = js.undefined
  val onChange:                 js.UndefOr[js.Function1[ReactEventFromInput, Unit]]               = js.undefined

  // Attributes with "html" prefix - forwarded into HTML-tags.
  val htmlFor: js.UndefOr[String] = js.undefined

}

object MuiPropsBaseStatic {
  def empty[T <: js.Object]: T =
    js.Object().asInstanceOf[T]
}


trait MuiPropsBaseClasses[Classes_t <: js.Object] extends js.Object {
  val classes: js.UndefOr[Classes_t] = js.undefined
}
trait MuiPropsBaseComponent extends js.Object {
  val component: js.UndefOr[Component_t] = js.undefined
}


trait MuiClassesBase extends js.Object {
  val root: js.UndefOr[String] = js.undefined
}