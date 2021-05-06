package net.xdsoft.jodit.types

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|


trait ILanguageOptions extends js.Object {
  val language: js.UndefOr[String] = js.undefined
  val debugLanguage: js.UndefOr[Boolean] = js.undefined
  val i18n: js.UndefOr[js.Dictionary[String] | Boolean] = js.undefined
}


trait IToolbarOptions extends js.Object {

  val theme: js.UndefOr[String] = js.undefined
  val toolbar: js.UndefOr[Boolean | String | dom.html.Element] = js.undefined
  val toolbarButtonSize: js.UndefOr[js.Any /*IUIButtonState['size']*/] = js.undefined
  val textIcons: js.UndefOr[Boolean | js.Function1[String, Boolean]] = js.undefined

  val extraButtons: js.UndefOr[Buttons] = js.undefined
  val removeButtons: js.UndefOr[js.Array[String]] = js.undefined
  val extraIcons:  js.UndefOr[js.Dictionary[String]] = js.undefined

  val buttons: js.UndefOr[ButtonsOption] = js.undefined

  val showTooltip: js.UndefOr[Boolean] = js.undefined
  val showTooltipDelay: js.UndefOr[Double] = js.undefined
  val useNativeTooltip: js.UndefOr[Boolean] = js.undefined

  val direction: js.UndefOr[IToolbarOptions.Direction] = js.undefined

}
object IToolbarOptions {
  type Direction <: String
  object Direction {
    final def rtl = "rtl".asInstanceOf[Direction]
    final def ltr = "ltr".asInstanceOf[Direction]
    final def auto = "".asInstanceOf[Direction]
  }
}


trait IViewOptions extends ILanguageOptions with IToolbarOptions {
  val headerButtons: js.UndefOr[String | js.Array[IControlType | String | ButtonsGroup]] = js.undefined
  val basePath: js.UndefOr[String] = js.undefined

  val defaultTimeout: js.UndefOr[Double] = js.undefined

  val disabled,
      readonly,
      iframe: js.UndefOr[Boolean] = js.undefined

  val namespace: js.UndefOr[String] = js.undefined

  val activeButtonsInReadOnly: js.UndefOr[js.Array[String]] = js.undefined

  val allowTabNavigation: js.UndefOr[Boolean] = js.undefined

  val zIndex: js.UndefOr[Double] = js.undefined

  val fullsize,
      globalFullSize: js.UndefOr[Boolean] = js.undefined

  val controls: js.UndefOr[Controls] = js.undefined

  val createAttributes: js.UndefOr[js.Dictionary[Attributes | dom.html.Element => Unit]] = js.undefined

  val events: js.UndefOr[js.Dictionary[js.Function]] = js.undefined

  val shadowRoot: js.UndefOr[js.Any /*Nullable[ShadowRoot]*/] = js.undefined

  val ownerWindow: js.UndefOr[dom.Window] = js.undefined
}
