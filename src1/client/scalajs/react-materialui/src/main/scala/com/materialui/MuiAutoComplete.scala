package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.2020 22:49
  * Description:
  * @see [[https://material-ui.com/components/autocomplete/]]
  */
object MuiAutoComplete {

  val component = JsForwardRefComponent[Props[_], Children.None, dom.html.Element]( Mui.AutoComplete )

  def apply[T]( p: Props[T] ) =
    component( p )


  trait Props[T]
    extends MuiPropsBase
    with MuiPropsBaseClasses[Classes]
  {
    val autoComplete,
        autoHighlight,
        autoSelect,
        clearOnBlur,
        clearOnEscape,
        debug,
        disableClearable,
        disableCloseOnSelect,
        disabled,
        disabledItemsFocusable,
        disableListWrap,
        disablePortal,
        filterSelectedOptions,
        freeSolo,
        fullWidth,
        handleHomeEndKeys,
        includeInputInList,
        loading,
        multiple,
        open,
        openOnFocus,
        selectOnFocus
        : js.UndefOr[Boolean] = js.undefined
    val blurOnSelect: js.UndefOr[BlurOnSelect] = js.undefined
    val ChipProps: js.UndefOr[MuiChipProps] = js.undefined
    val clearText,
        closeText,
        id,
        inputValue,
        openText
        : js.UndefOr[String] = js.undefined
    val clearIcon,
        loadingText,
        noOptionsText,
        popupIcon
        : js.UndefOr[raw.React.Node] = js.undefined
    val defaultValue: js.UndefOr[js.Any] = js.undefined
    /** A filter function that determines the options that are eligible.
      * `function(options: T[], state: object) => undefined`
      * - `options`: The options to render.
      * - `state`: The state of the component.
      */
    val filterOptions: js.UndefOr[js.Function2[js.Array[T], js.Object, js.Array[js.Any]]] = js.undefined
    val forcePopupIcon: js.UndefOr[ForcePopupIcon] = js.undefined
    val getLimitTagsText: js.UndefOr[js.Function1[Int, raw.React.Node]] = js.undefined
    val getOptionDisabled: js.UndefOr[js.Function1[T, Boolean]] = js.undefined
    // TODO T|String - при нажатии Enter в input'е - функция вызывается над строкой из input'а. В остальных случаях - T.
    val getOptionLabel: js.UndefOr[js.Function1[T | String, String]] = js.undefined
    val getOptionSelected: js.UndefOr[js.Function2[T, T, Boolean]] = js.undefined
    val groupBy: js.UndefOr[js.Function1[T, String]] = js.undefined
    val limitTags: js.UndefOr[Int] = js.undefined
    val ListboxComponent: js.UndefOr[js.Any] = js.undefined
    val ListboxProps: js.UndefOr[js.Object] = js.undefined
    @JSName("onChange")
    val onChange4: js.UndefOr[js.Function4[ReactEvent, T | js.Array[T] | Null, OnChangeReason, js.UndefOr[OnChangeDetails[T]], Unit]] = js.undefined
    val onClose: js.UndefOr[js.Function2[ReactEvent, OnCloseReason, Unit]] = js.undefined
    val onHighlightChange: js.UndefOr[js.Function3[ReactEvent, T, OnHighlightChangeReason, Unit]] = js.undefined
    val onInputChange: js.UndefOr[js.Function3[ReactEventFromInput, String, OnInputChangeReason, Unit]] = js.undefined
    val onOpen: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
    val options: js.Array[T]
    val PaperComponent: js.UndefOr[js.Any] = js.undefined
    val PopperComponent: js.UndefOr[js.Any] = js.undefined
    val renderGroup: js.UndefOr[js.Function1[js.Any, raw.React.Node]] = js.undefined
    val renderInput: js.Function1[MuiTextFieldProps, raw.React.Node]
    val renderOption: js.UndefOr[js.Function3[js.Object, T, js.Object, raw.React.Node]] = js.undefined
    val renderTags: js.UndefOr[js.Function2[js.Array[T], js.Function1[String, js.Any], raw.React.Node]] = js.undefined
    val size: js.UndefOr[Size] = js.undefined
    val value: js.UndefOr[T] = js.undefined
  }


  trait OnChangeDetails[T] extends js.Object {
    val option: T
  }


  type BlurOnSelect <: js.Any
  object BlurOnSelect {
    final def NOT_BLURRED = false.asInstanceOf[BlurOnSelect]
    final def ALWAYS_BLURRED = true.asInstanceOf[BlurOnSelect]
    final def BLUR_AFTER_TOUCH = "touch".asInstanceOf[BlurOnSelect]
    final def BLUR_AFTER_MOUSE = "mouse".asInstanceOf[BlurOnSelect]
  }


  type ForcePopupIcon <: js.Any
  object ForcePopupIcon {
    final def AUTO = "auto".asInstanceOf[ForcePopupIcon]
    final def TRUE = true.asInstanceOf[ForcePopupIcon]
    final def FALSE = false.asInstanceOf[ForcePopupIcon]
  }


  type OnChangeReason <: String
  object OnChangeReason {
    final def CREATE_OPTION = "create-option".asInstanceOf[OnChangeReason]
    final def SELECT_OPTION = "select-option".asInstanceOf[OnChangeReason]
    final def REMOVE_OPTION = "remove-option".asInstanceOf[OnChangeReason]
    final def BLUR = "blur".asInstanceOf[OnChangeReason]
    final def CLEAR = "clear".asInstanceOf[OnChangeReason]
  }


  type OnCloseReason <: String
  object OnCloseReason {
    final def TOGGLE_INPUT = "toggleInput".asInstanceOf[OnCloseReason]
    final def ESCAPE = "escape".asInstanceOf[OnCloseReason]
    final def SELECT_OPTION = "select-option".asInstanceOf[OnCloseReason]
    final def BLUR = "blur".asInstanceOf[OnCloseReason]
  }


  type OnHighlightChangeReason <: String
  object OnHighlightChangeReason {
    final def KEYBOARD = "keyboard".asInstanceOf[OnHighlightChangeReason]
    final def AUTO = "auto".asInstanceOf[OnHighlightChangeReason]
    final def MOUSE = "mouse".asInstanceOf[OnHighlightChangeReason]
  }


  type OnInputChangeReason <: String
  object OnInputChangeReason {
    final def INPUT = "input".asInstanceOf[OnInputChangeReason]
    final def RESET = "reset".asInstanceOf[OnInputChangeReason]
    final def CLEAR = "clear".asInstanceOf[OnInputChangeReason]
  }


  type Size <: String
  object Size {
    final def MEDIUM = "medium".asInstanceOf[Size]
    final def SMALL = "small".asInstanceOf[Size]
  }


  /** CSS для [[MuiAutoComplete]]. */
  trait Classes
    extends MuiClassesBase
  {
    val fullWidth,
        focused,
        tag,
        tagSizeSmall,
        hasPopupIcon,
        hasClearIcon,
        inputRoot,
        input,
        inputFocused,
        endAdornment,
        clearIndicator,
        clearIndicatorDirty,
        popupIndicator,
        popupIndicatorOpen,
        popper,
        popperDisablePortal,
        paper,
        listbox,
        loading,
        noOptions,
        option,
        groupLabel,
        groupUl
        : js.UndefOr[String] = js.undefined
  }

}
