package io.suggest.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import io.suggest.sc.model.grid.{GridAdKey_t, MGridCoreS}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Element

/** react-stonecutter grid rendering implementation. */
final class StoneCutterGridRenderer extends IGridRenderer {

  override def apply(mgridCore: MGridCoreS)(children: Iterator[(GridAdKey_t, TagOf[Element])]): VdomElement = {
    CSSGrid {
      GridBuilderUtilJs.mkCssGridArgs(
        gbRes     = mgridCore.gridBuild,
        conf      = mgridCore.jdConf,
        tagName   = GridComponents.DIV,
        info      = mgridCore.info,
      )
    } (
      children
        .map(_._2)
        .toVdomArray,
    )
  }

  override def preferLinkContainer = false

}
