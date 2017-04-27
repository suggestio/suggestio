package react.image.gallery

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.17 18:35
  * Description: Image gallery Item.
  */
@ScalaJSDefined
trait IgItem extends js.Object {

  /** image src url */
  val original: String

  /** image thumbnail src url */
  val thumbnail: UndefOr[String] = js.undefined

  /** custom image class */
  val originalClass: UndefOr[String] = js.undefined

  /** custom thumbnail class */
  val thumbnailClass: UndefOr[String] = js.undefined

  /** image alt */
  val originalAlt: UndefOr[String] = js.undefined

  /** thumbnail image alt */
  val thumbnailAlt: UndefOr[String] = js.undefined

  /** label for thumbnail */
  val thumbnailLabel: UndefOr[String] = js.undefined

  /** description for image */
  val description: UndefOr[String] = js.undefined

  /** image srcset (html5 attribute) */
  val srcSet: UndefOr[String] = js.undefined

  /** image sizes (html5 attribute) */
  val sizes: UndefOr[String] = js.undefined

}
