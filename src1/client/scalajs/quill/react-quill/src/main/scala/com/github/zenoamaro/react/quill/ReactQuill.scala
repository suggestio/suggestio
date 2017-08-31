package com.github.zenoamaro.react.quill

import com.quilljs.delta.Delta
import com.quilljs.quill.events.{KeyDownEvent, KeyPressEvent, KeyUpEvent}
import com.quilljs.quill.Quill
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 12:53
  * Description: Scala.js bindings for react-quill component.
  */
object ReactQuill {

  val component = JsComponent[ReactQuillPropsR, Children.None, Null]( ReactQuillJs )

  def apply(props: ReactQuillPropsR = new ReactQuillPropsR {}) = component(props)

}


/** Примонтированный компонент обладает кое-какими методами. */
@js.native
trait ReactQuillJsMounted extends js.Object {

  def focus(): Unit = js.native

  def blur(): Unit = js.native

  def getEditor(): Quill = js.native

}


/** Non-native react-quill js-component facade. */
@JSImport("react-quill", JSImport.Namespace)
@js.native
object ReactQuillJs extends js.Object



/** react-quill component properties JSON. */
@ScalaJSDefined
trait ReactQuillPropsR extends js.Object {

  val id : UndefOr[String] = js.undefined

  val className: UndefOr[String] = js.undefined

  /** Value for the editor as a controlled component.
    * Can be a string containing HTML, a Quill Delta instance, or a plain object representing a Delta. */
  val value : UndefOr[ContentValue_t] = js.undefined

  /** Initial value for the editor as an uncontrolled component.
    * Can be a string containing HTML, a Quill Delta instance, or a plain object representing a Delta. */
  val defaultValue: UndefOr[ContentValue_t] = js.undefined

  val readOnly: UndefOr[Boolean] = js.undefined

  /** The default value for the empty editor. */
  val placeholder: UndefOr[ContentValue_t] = js.undefined

  /** An object specifying which modules are enabled, and their configuration.
    * @see [[http://quilljs.com/docs/modules/]]
    */
  val modules: UndefOr[js.Object] = js.undefined

  val formats: UndefOr[js.Array[String]] = js.undefined

  /** An object with custom CSS rules to apply on the editor's container.
    * Rules should be in React's "camelCased" naming style.
    */
  val style: UndefOr[js.Object] = js.undefined

  val theme: UndefOr[String] = js.undefined

  val tabIndex: UndefOr[Int] = js.undefined

  /** Selector or DOM element used by Quill to constrain position of popups.
    * Defaults to document.body. */
  val bounds: UndefOr[Bounds_t] = js.undefined

  val onChange: UndefOr[js.Function4[Html_t, Delta, Source_t, QuillUnpriveledged, _]] = js.undefined

  val onChangeSelection: UndefOr[js.Function3[Range_t, Source_t, QuillUnpriveledged, _]] = js.undefined

  val onFocus: UndefOr[js.Function3[Range_t, Source_t, QuillUnpriveledged, _]] = js.undefined

  val onBlur: UndefOr[js.Function3[Range_t, Source_t, QuillUnpriveledged, _]] = js.undefined

  val onKeyPress: UndefOr[js.Function1[KeyPressEvent, _]] = js.undefined

  val onKeyDown: UndefOr[js.Function1[KeyDownEvent, _]] = js.undefined

  val onKeyUp: UndefOr[js.Function1[KeyUpEvent, _]] = js.undefined

}
