package react.image.gallery

import japgolly.scalajs.react.{JsComponent, Children}
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.17 18:30
  * Description: Image gallery class facade.
  *
  * @see [[https://www.npmjs.com/package/react-image-gallery]]
  */

object ImageGalleryR {

  val component = JsComponent[ImageGalleryPropsR, Children.None, Null]( ImageGalleryC )

  def apply( props: ImageGalleryPropsR ) = component( props )

}


@JSImport("react-image-gallery", JSImport.Default)
@js.native
object ImageGalleryC extends js.Object {

  def play(): Unit = js.native

  def pause(): Unit = js.native

  def fullScreen(): Unit = js.native

  def exitFullScreen(): Unit = js.native

  def slideToIndex(index: Int): Unit = js.native

  def getCurrentIndex(): Int = js.native

}


/**
  * JSON properties model.
  */
@ScalaJSDefined
trait ImageGalleryPropsR extends js.Object {

  val items: js.Array[IgItem]

  val infinite: UndefOr[Boolean] = js.undefined

  val lazyLoad: UndefOr[Boolean] = js.undefined

  val showNav: UndefOr[Boolean] = js.undefined

  val showThumbnails: UndefOr[Boolean] = js.undefined

  val thumbnailPosition: UndefOr[ThumbnailPositions.T] = js.undefined

  val showFullscreenButton: UndefOr[Boolean] = js.undefined

  val useBrowserFullscreen: UndefOr[Boolean] = js.undefined

  val showPlayButton: UndefOr[Boolean] = js.undefined

  val showBullets: UndefOr[Boolean] = js.undefined

  val showIndex: UndefOr[Boolean] = js.undefined

  val autoPlay: UndefOr[Boolean] = js.undefined

  val disableThumbnailScroll: UndefOr[Boolean] = js.undefined

  val slideOnThumbnailHover: UndefOr[Boolean] = js.undefined

  val disableArrowKeys: UndefOr[Boolean] = js.undefined

  val disableSwipe: UndefOr[Boolean] = js.undefined

  val defaultImage: UndefOr[String] = js.undefined

  val onImageError: UndefOr[js.Function1[Event, _]] = js.undefined

  val onThumbnailError: UndefOr[js.Function1[Event, _]] = js.undefined

  val indexSeparator: UndefOr[String] = js.undefined

  /** 450 ms. */
  val slideDuration: UndefOr[Int] = js.undefined

  /** 0 ms */
  val swipingTransitionDuration: UndefOr[Int] = js.undefined

  /** 3000 ms */
  val slideInterval: UndefOr[Int] = js.undefined

  val startIndex: UndefOr[Int] = js.undefined

  val onImageLoad: UndefOr[js.Function1[Event,_]] = js.undefined

  /** $1 = currentIndex */
  val onSlide: UndefOr[js.Function1[Int, _]] = js.undefined

  val onScreenChange: UndefOr[js.Function1[HTMLElement, _]] = js.undefined

  /** $1 = currentIndex */
  val onPause: UndefOr[js.Function1[Int, _]] = js.undefined

  /** $1 = currentIndex */
  val onPlay: UndefOr[js.Function1[Int, _]] = js.undefined

  val onClick: UndefOr[js.Function1[Event, _]] = js.undefined


  // TODO implement render*(...) function interfaces.
}
