package net.xdsoft.jodit

import net.xdsoft.jodit.types.{ButtonsOption, IExtraPlugin, IViewOptions}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|


trait IConfig extends IViewOptions {

  val commandToHotkeys: js.UndefOr[js.Dictionary[String | js.Array[String]]] = js.undefined

  val license,
      preset
      : js.UndefOr[String] = js.undefined

  val presets: js.UndefOr[js.Dictionary[js.Object]] = js.undefined

  val ownerDocument: js.UndefOr[dom.html.Document] = js.undefined

  val inline,
      saveModeInStorage,
      spellcheck,
      triggerChangeEvent,
      statusbar,
      useSplitMode
      : js.UndefOr[Boolean] = js.undefined

  val editorCssClass: js.UndefOr[Boolean | String] = js.undefined
  val style: js.UndefOr[Boolean | js.Dictionary[js.Any]] = js.undefined
  val tabIndex: js.UndefOr[Int] = js.undefined


  val enter: js.UndefOr[IConfig.Enter] = js.undefined
  val enterBlock: js.UndefOr[IConfig.EnterBlock] = js.undefined

  val defaultMode: js.UndefOr[EditorMode] = js.undefined

  val colors: js.UndefOr[js.Dictionary[js.Array[String]] | js.Array[String]] = js.undefined
  val colorPickerDefaultTab: js.UndefOr[IConfig.ColorPickerDefaultTab] = js.undefined
  val imageDefaultWidth: js.UndefOr[Int] = js.undefined
  val disablePlugins: js.UndefOr[String | js.Array[String]] = js.undefined
  val extraPlugins: js.UndefOr[js.Array[String | IExtraPlugin]] = js.undefined

  val sizeLG, sizeMD, sizeSM: js.UndefOr[Int] = js.undefined
  val buttonsMD, buttonsSM, buttonsXS: js.UndefOr[ButtonsOption] = js.undefined
  val showBrowserColorPicker: js.UndefOr[Boolean] = js.undefined

}


object IConfig {

  type Enter <: String
  object Enter {
    final def p = "p".asInstanceOf[Enter]
    final def div = "div".asInstanceOf[Enter]
    final def br = "br".asInstanceOf[Enter]
  }

  type EnterBlock <: String
  object EnterBlock {
    final def p = Enter.p.asInstanceOf[EnterBlock]
    final def div = Enter.div.asInstanceOf[EnterBlock]
  }

  type ColorPickerDefaultTab <: String
  object ColorPickerDefaultTab {
    final def background = "background".asInstanceOf[ColorPickerDefaultTab]
    final def color = "color".asInstanceOf[ColorPickerDefaultTab]
  }

}
