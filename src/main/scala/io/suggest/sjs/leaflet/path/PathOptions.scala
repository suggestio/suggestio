package io.suggest.sjs.leaflet.path

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 14:26
  * Description: API for path options.
  */
object PathOptions extends FromDict {
  override type T = PathOptions
}


@js.native
trait PathOptions extends js.Object {

  //true   Whether to draw stroke along the path. Set it to false to disable borders on polygons or circles.
  var stroke          : Boolean   = js.native

  //'#03f' 	Stroke color.
  var color           : String    = js.native

  //5 	Stroke width in pixels.
  var weight          : Int       = js.native

  //0.5 	Stroke opacity.
  var opacity         : Double    = js.native

  //depends 	Whether to fill the path with color. Set it to false to disable filling on polygons or circles.
  var fill            : Boolean   = js.native

  //[same as color] 	Fill color.
  var fillColor       : String    = js.native

  //0.2 	Fill opacity.
  var fillOpacity     : Double    = js.native

  //'evenodd' 	A string that defines how the inside of a shape is determined.
  var fillRule        : String 	  = js.native

  //null 	A string that defines the stroke dash pattern. Doesn't work on canvas-powered layers (e.g. Android 2).
  var dashArray       : String 	  = js.native

  //null 	A string that defines shape to be used at the end of the stroke.
  var lineCap         : String 	  = js.native

  //null 	A string that defines shape to be used at the corners of the stroke.
  var lineJoin        : String    = js.native

  //true 	If false, the vector will not emit mouse events and will act as a part of the underlying map.
  var clickable       : Boolean   = js.native

  //null 	Sets the pointer-events attribute on the path if SVG backend is used.
  var pointerEvents   : String 	  = js.native

  //'' 	Custom class name set on an element.
  var className       : String    = js.native

}
