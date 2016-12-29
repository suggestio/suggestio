package io.suggest.sjs.leaflet.path

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 14:26
  * Description: API for path options.
  */
object PathOptions extends FromDict {
  override type T = PathOptions
}


/** JSON for declaring Path style.
  *
  * For scala 0.6.14+.
  */
@ScalaJSDefined
trait PathOptions extends js.Object {

  //true   Whether to draw stroke along the path. Set it to false to disable borders on polygons or circles.
  val stroke          : js.UndefOr[Boolean]   = js.undefined

  //'#03f' 	Stroke color.
  val color           : js.UndefOr[String]    = js.undefined

  //5 	Stroke width in pixels.
  val weight          : js.UndefOr[Int]       = js.undefined

  //0.5 	Stroke opacity.
  val opacity         : js.UndefOr[Double]    = js.undefined

  //depends 	Whether to fill the path with color. Set it to false to disable filling on polygons or circles.
  val fill            : js.UndefOr[Boolean]   = js.undefined

  //[same as color] 	Fill color.
  val fillColor       : js.UndefOr[String]    = js.undefined

  //0.2 	Fill opacity.
  val fillOpacity     : js.UndefOr[Double]    = js.undefined

  //'evenodd' 	A string that defines how the inside of a shape is determined.
  val fillRule        : js.UndefOr[String]    = js.undefined

  //null 	A string that defines the stroke dash pattern. Doesn't work on canvas-powered layers (e.g. Android 2).
  val dashArray       : js.UndefOr[String]    = js.undefined

  //null 	A string that defines shape to be used at the end of the stroke.
  val lineCap         : js.UndefOr[String]    = js.undefined

  //null 	A string that defines shape to be used at the corners of the stroke.
  val lineJoin        : js.UndefOr[String]    = js.undefined

  //true 	If false, the vector will not emit mouse events and will act as a part of the underlying map.
  val clickable       : js.UndefOr[Boolean]   = js.undefined

  //null 	Sets the pointer-events attribute on the path if SVG backend is used.
  val pointerEvents   : js.UndefOr[String]    = js.undefined

  //'' 	Custom class name set on an element.
  val className       : js.UndefOr[String]    = js.undefined

}
