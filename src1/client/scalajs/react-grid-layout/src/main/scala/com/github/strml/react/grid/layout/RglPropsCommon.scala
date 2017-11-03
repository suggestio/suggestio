package com.github.strml.react.grid.layout

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 17:22
  * Description: Common RGL props, shared between [[ReactGridLayout]] and [[Responsive]] components' props.
  */
trait RglPropsCommon extends js.Object {

  //
  // Basic props
  //

  /**
    * This allows setting the initial width on the server side.
    * This is required unless using the HOC <WidthProvider> or similar
    */
  val width               : UndefOr[Int]                          = js.undefined

  /** If true, the container height swells and contracts to fit contents. */
  val autoSize            : UndefOr[Boolean]                      = js.undefined

  /**
    * A CSS selector for tags that will not be draggable.
    * For example: draggableCancel:'.MyNonDraggableAreaClassName'
    * If you forget the leading . it will not work.
    */
  val draggableCancel     : UndefOr[String]                       = js.undefined

  /**
    * A CSS selector for tags that will act as the draggable handle.
    * For example: draggableHandle:'.MyDragHandleClassName'
    * If you forget the leading . it will not work.
    */
  val draggableHandle     : UndefOr[String]                       = js.undefined

  /** If true, the layout will compact vertically. */
  val verticalCompact     : UndefOr[Boolean]                      = js.undefined

  /** Compaction type. */
  val compactType         : UndefOr[CompactType_t]                = js.undefined

  /** Margin between items [x, y] in px. Default: [10, 10]. */
  val margin              : js.UndefOr[js.Array[Int]]             = js.undefined

  /** Padding inside the container [x, y] in px. Default: margin. */
  val containerPadding    : js.UndefOr[js.Array[Int]]             = js.undefined

  /** Rows have a static height, but you can change this based on breakpoints if you like. [150]. */
  val rowHeight           : js.UndefOr[Int]                       = js.undefined


  //
  // Flags
  //

  val isDraggable         : js.UndefOr[Boolean]                   = js.undefined

  val isResizable         : js.UndefOr[Boolean]                   = js.undefined

  /** Uses CSS3 translate() instead of position top/left.
    * This makes about 6x faster paint performance. [true] */
  val useCSSTransforms    : js.UndefOr[Boolean]                   = js.undefined

  /** If true, grid items won't change position when being dragged over [false]. */
  val preventCollision    : js.UndefOr[Boolean]                   = js.undefined


  //
  // Callbacks
  //

  val onDragStart         : js.UndefOr[ItemCallback]              = js.undefined

  val onDrag              : js.UndefOr[ItemCallback]              = js.undefined

  val onDragStop          : js.UndefOr[ItemCallback]              = js.undefined

  val onResizeStart       : js.UndefOr[ItemCallback]              = js.undefined

  val onResize            : js.UndefOr[ItemCallback]              = js.undefined

  val onResizeStop        : js.UndefOr[ItemCallback]              = js.undefined

}
