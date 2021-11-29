package io.suggest.grid

import io.suggest.sc.model.grid.{GridAdKey_t, MGridCoreS}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Element

class PlainGridRenderer extends IGridRenderer {

  override def apply(mgrid: MGridCoreS)
                    (children: Iterator[(GridAdKey_t, TagOf[Element])]): VdomElement = {
    val _positionAbsolute = ^.position.absolute
    val _top = ^.top
    val _left = ^.left

    <.div(
      ^.position.relative,
      ^.width := 100.pc,
      ^.height := mgrid.gridBuild.gridWh.height.px,

      children
        .zip( mgrid.gridBuild.coords )
        .map { case ((key, gridElBody), coords) =>
          <.div(
            ^.key := key,

            _positionAbsolute,
            _top := coords.topLeft.y.px,
            _left := coords.topLeft.x.px,

            gridElBody,
          )
        }
        .toVdomArray,
    )
  }

  override def preferLinkContainer: Boolean = true

}
