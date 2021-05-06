package net.xdsoft.jodit.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import net.xdsoft.jodit.IConfig
import net.xdsoft.jodit.types.IJodit

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport


object JoditEditor {

  val component = JsForwardRefComponent[Props, Children.None, IJodit]( Js )

  @js.native
  @JSImport("jodit-react", JSImport.Default)
  object Js extends js.Object


  trait Props extends js.Object {

    val config: js.UndefOr[IConfig] = js.undefined

    val id, name, value: js.UndefOr[String] = js.undefined

    val onBlur: js.UndefOr[js.Function2[String, ReactFocusEvent, Unit]] = js.undefined
    val onChange: js.UndefOr[js.Function1[String, Unit]] = js.undefined

    val editorRef: js.UndefOr[raw.React.RefFn[IJodit]] = js.undefined

    val tabIndex: js.UndefOr[Int] = js.undefined

  }

}

