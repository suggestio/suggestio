package tinymce

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, undefined, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.08.17 10:19
  * Description: JSON config for tinyMCE editor.
  */

/** tinyMCE JSON config part with params, suitable for both react and plain use-cases. */
trait TinyMceConfigCommon extends js.Object {

  val width                   : UndefOr[Int]      = undefined
  val height                  : UndefOr[Int]      = undefined

  val menubar                 : UndefOr[Boolean]  = undefined

  val statusbar               : UndefOr[Boolean]  = undefined

  val content_css             : UndefOr[String]   = undefined

  val fontsize_formats        : UndefOr[String]   = undefined

  val style_formats           : UndefOr[String]   = undefined

  val language                : UndefOr[String]   = undefined

  val font_size_style_values  : UndefOr[String]   = undefined

  val setup                   : UndefOr[js.Function1[TinyMce, _]] = undefined

  /** plugins: 'link, textcolor, paste, colorpicker' */
  val plugins                 : UndefOr[String]   = undefined

  /** toolbar: ["styleselect | fontsizeselect | alignleft aligncenter alignright | bold italic | colorpicker | link | removeformat"] */
  val toolbar                 : UndefOr[String | js.Array[String]] = undefined

}


/** Конфиг tinyMCE для прямой инициализации (без react-обёртки). */
trait TinyMceConfig extends TinyMceConfigCommon {

  /** tinymce rendering target. */
  val selector  : String

}
