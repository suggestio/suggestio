package com.github.dominictobias.react.image.crop

import japgolly.scalajs.react._
import org.scalajs.dom.Element
import org.scalajs.dom.raw.Blob

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|
/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 15:30
  * Description: React crop component facade.
  */
object ReactCrop {

  val component = JsComponent[ReactCropProps, Children.None, PercentCrop]( ReactCropJs )

  def apply( props: ReactCropProps ) = component( props )

}


/** JS-компонент. */
@js.native
@JSImport("react-image-crop", JSImport.Namespace)
protected object ReactCropJs extends js.Object


/** Props for [[ReactCrop]] */
trait ReactCropProps extends js.Object {

  val src             : String | Blob

  val crop            : js.UndefOr[PercentCrop]     = js.undefined

  val minWidth        : js.UndefOr[Percentage_t]    = js.undefined
  val minHeight       : js.UndefOr[Percentage_t]    = js.undefined

  val maxWidth        : js.UndefOr[Percentage_t]    = js.undefined
  val maxHeight       : js.UndefOr[Percentage_t]    = js.undefined

  val keepSelection   : js.UndefOr[Boolean]         = js.undefined
  val disabled        : js.UndefOr[Boolean]         = js.undefined

  val onChange        : js.UndefOr[js.Function2[PercentCrop, PixelCrop, _]]             = js.undefined
  val onComplete      : js.UndefOr[js.Function2[PercentCrop, PixelCrop, _]]             = js.undefined
  val onImageLoaded   : js.UndefOr[js.Function3[PercentCrop, Element, PixelCrop, _]]    = js.undefined
  val onAspectRatioChange: js.UndefOr[js.Function2[PercentCrop, PixelCrop, _]]          = js.undefined
  val onDragStart     : js.UndefOr[js.Function0[_]]                                     = js.undefined
  val onDragEnd       : js.UndefOr[js.Function0[_]]                                     = js.undefined

  val crossorigin     : js.UndefOr[String] = js.undefined

}
