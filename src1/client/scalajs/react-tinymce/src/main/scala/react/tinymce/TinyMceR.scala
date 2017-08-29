package react.tinymce

import japgolly.scalajs.react.{Children, JsComponent}
import tinymce.TinyMceConfigCommon
import tinymce.events.TinyMceEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.17 10:11
  * Description: Sjs-facade for react-tinymce/TinyMCE component.
  */

object TinyMceR {

  val component = JsComponent[TinyMcePropsR, Children.None, Null]( TinyMceJsR )

  def apply(props: TinyMcePropsR) = component( props )

}


/** js-компонент из react-tinymce. */
// Судя по докам, должно быть @JSImport("react-tinymce", "TinyMCE"). Но по факту, нужно импортировать корень.
@JSImport("react-tinymce", JSImport.Namespace)
@js.native
object TinyMceJsR extends js.Object



/** TinyMCE component properties. */
@ScalaJSDefined
trait TinyMcePropsR extends js.Object {

  /** HTML-контент в редакторе. */
  val content     : String

  val config      : js.UndefOr[TinyMceConfigCommon]           = js.undefined

  val onChange    : js.UndefOr[js.Function1[TinyMceEvent, _]] = js.undefined

  // TODO add other events.
}
