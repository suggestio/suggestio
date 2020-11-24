package com.github.strml.react.resizable

import scala.scalajs.js
import japgolly.scalajs.react._
import org.scalajs.dom.html

import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.18 22:12
  * Description: Properties for [[Resizable]] and [[ResizableBox]].
  */
trait ResizableProps extends js.Object {

  val width: Double
  val height : Double

  val className: js.UndefOr[String] = js.undefined

  /** Either a ReactElement to be used as handle,
    * or a function returning an element that is fed the handle's location as its first argument. */
  val handle: js.UndefOr[raw.React.Element | js.Function1[ResizableProps.Handle, raw.React.Element]] = js.undefined

  /** If you change this, be sure to update your css
    * default: [10, 10]
    */
  val handleSize: js.UndefOr[js.Array[Double]] = js.undefined

  // false
  val lockAspectRatio: js.UndefOr[Boolean] = js.undefined

  // See [[Axis]].
  val axis: js.UndefOr[ResizableProps.Axis] = js.undefined

  // [10, 10],
  val minConstraints: js.UndefOr[js.Array[Double]] = js.undefined

  // [Infinity, Infinity],
  val maxConstraints: js.UndefOr[js.Array[Double]] = js.undefined

  // (e: SyntheticEvent, data: ResizeCallbackData) => any,
  val onResizeStop: js.UndefOr[ResizableProps.Cb] = js.undefined

  // (e: SyntheticEvent, data: ResizeCallbackData) => any,
  val onResizeStart: js.UndefOr[ResizableProps.Cb] = js.undefined

  // (e: SyntheticEvent, data: ResizeCallbackData) => any,
  val onResize: js.UndefOr[ResizableProps.Cb] = js.undefined

  val draggableOpts: js.UndefOr[js.Object] = js.undefined

  val resizeHandles: js.UndefOr[js.Array[ResizableProps.Handle]] = js.undefined

  val transformScale: js.UndefOr[Double] = js.undefined

}
trait ResizableBoxProps extends ResizableProps {
  val style: js.UndefOr[js.Object] = js.undefined
}

object ResizableProps {

  type Cb = js.Function2[ReactEvent, ResizeCallbackData, _]

  type Axis <: String
  object Axis {
    final def BOTH = "both".asInstanceOf[Axis]
    final def X = "x".asInstanceOf[Axis]
    final def Y = "y".asInstanceOf[Axis]
    final def NONE = "none".asInstanceOf[Axis]
  }


  type Handle <: String
  object Handle {
    final def S = "s".asInstanceOf[Handle]
    final def W = "w".asInstanceOf[Handle]
    final def E = "e".asInstanceOf[Handle]
    final def N = "n".asInstanceOf[Handle]
    final def SW = "sw".asInstanceOf[Handle]
    final def NW = "nw".asInstanceOf[Handle]
    final def SE = "se".asInstanceOf[Handle]
    final def NE = "ne".asInstanceOf[Handle]
  }

}


@js.native
trait Wh extends js.Object {
  val width: Double = js.native
  val height: Double = js.native
}


@js.native
trait ResizeCallbackData extends js.Object {
  val node: html.Element = js.native
  val size: Wh = js.native
  val handle: ResizableProps.Handle
}
