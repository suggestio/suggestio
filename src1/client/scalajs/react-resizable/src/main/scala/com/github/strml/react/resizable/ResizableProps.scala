package com.github.strml.react.resizable

import scala.scalajs.js
import japgolly.scalajs.react.ReactEvent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.18 22:12
  * Description: Properties for [[Resizable]] and [[ResizableBox]].
  */
trait ResizableProps extends js.Object {

  val width: Double
  val height : Double

  // If you change this, be sure to update your css

  // [10, 10]
  val handleSize: js.UndefOr[js.Array[Double]] = js.undefined

  // false
  val lockAspectRatio: js.UndefOr[Boolean] = js.undefined

  // See [[Axis]].
  val axis: js.UndefOr[String] = js.undefined

  // [10, 10],
  val minConstraints: js.UndefOr[js.Array[Double]] = js.undefined

  // [Infinity, Infinity],
  val maxConstraints: js.UndefOr[js.Array[Double]] = js.undefined

  // (e: SyntheticEvent, data: ResizeCallbackData) => any,
  val onResizeStop: js.UndefOr[js.Function2[ReactEvent, ResizeCallbackData, _]] = js.undefined

  // (e: SyntheticEvent, data: ResizeCallbackData) => any,
  val onResizeStart: js.UndefOr[js.Function2[ReactEvent, ResizeCallbackData, _]] = js.undefined

  // (e: SyntheticEvent, data: ResizeCallbackData) => any,
  val onResize: js.UndefOr[js.Function2[ReactEvent, ResizeCallbackData, _]] = js.undefined

  val draggableOpts: js.UndefOr[js.Object] = js.undefined

}


object Axis {
  final def BOTH = "both"
  final def X = "x"
  final def Y = "y"
  final def NONE = "none"
}


@js.native
trait ResizeCallbackData extends js.Object
