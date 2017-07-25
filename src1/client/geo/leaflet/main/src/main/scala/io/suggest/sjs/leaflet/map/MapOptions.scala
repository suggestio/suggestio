package io.suggest.sjs.leaflet.map

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
 * Suggest.io
 * User: UndefOr[Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: UndefOr[19.11.15 16:54
 * Description: UndefOr[L.map() options object.
 * @see [[http://leafletjs.com/reference.html#map-options]]
 *
 * scalajs-0.6.14+ required.
 */
object MapOptions {

  /** Доп.утиль вокруг инстансов [[MapOptions]]. */
  implicit class MapOptionsExtOps[T <: MapOptions](val raw: T) extends AnyVal {

    /** Поработать с MapOptions как будто бы со словарём. */
    private def _mutateAsDict[U](f: js.Dictionary[js.Any] => U): T = {
      val jsDict = raw.asInstanceOf[js.Dictionary[js.Any]]
      f(jsDict)
      raw
    }

    /** Убрать attribution ИЗНУТРИ текущего инстанса. */
    def noAttribution: T = _mutateAsDict { jsDict =>
      jsDict("attributionControl") = false
    }

  }

}


/** JSON-модель опций сборки новой карты. */
@ScalaJSDefined
trait MapOptions extends js.Object {

  val center                : UndefOr[LatLng]           = js.undefined
  val zoom                  : UndefOr[Double]           = js.undefined
  val layers                : UndefOr[js.Array[Layer]]  = js.undefined
  val minZoom               : UndefOr[Double]           = js.undefined
  val maxZoom               : UndefOr[Double]           = js.undefined
  val maxBounds             : UndefOr[LatLngBounds]     = js.undefined

  // Interaction Options
  val dragging              : UndefOr[Boolean]          = js.undefined
  val touchZoom             : UndefOr[Boolean]          = js.undefined
  val scrollWheelZoom       : UndefOr[Boolean]          = js.undefined
  val doubleClickZoom       : UndefOr[Boolean]          = js.undefined
  val boxZoom               : UndefOr[Boolean]          = js.undefined
  val tap                   : UndefOr[Boolean]          = js.undefined
  val tapTolerance          : UndefOr[Int]              = js.undefined
  val trackResize           : UndefOr[Boolean]          = js.undefined
  val worldCopyJump         : UndefOr[Boolean]          = js.undefined
  val closePopupOnClick     : UndefOr[Boolean]          = js.undefined
  val bounceAtZoomLimits    : UndefOr[Boolean]          = js.undefined

  // Keyboard Navigation Options
  val keyboard              : UndefOr[Boolean]          = js.undefined
  val keyboardPanOffset     : UndefOr[Int]              = js.undefined
  val keyboardZoomOffset    : UndefOr[Int]              = js.undefined

  // Panning Inertia Options
  val inertia               : UndefOr[Boolean]          = js.undefined
  val inertiaDeceleration   : UndefOr[Double]           = js.undefined
  val inertiaMaxSpeed       : UndefOr[Double]           = js.undefined
  val inertiaThreshold      : UndefOr[Double]           = js.undefined

  // Control options
  val zoomControl           : UndefOr[Boolean]          = js.undefined
  val attributionControl    : UndefOr[Boolean]          = js.undefined

  // Animation options
  val fadeAnimation         : UndefOr[Boolean]          = js.undefined
  val zoomAnimation         : UndefOr[Boolean]          = js.undefined
  val zoomAnimationThreshold: UndefOr[Double]           = js.undefined
  val markerZoomAnimation   : UndefOr[Boolean]          = js.undefined

}
