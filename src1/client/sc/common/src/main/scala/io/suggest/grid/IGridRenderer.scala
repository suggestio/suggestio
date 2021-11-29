package io.suggest.grid

import io.suggest.sc.model.grid.{GridAdKey_t, MGridCoreS}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^.VdomElement
import org.scalajs.dom

trait IGridRenderer {

  /** Component-like abstraction for grid rendering implementation.
    * Due to problems with react-stonecuttor SSR (CSSGridItem returns null in beginning of animation),
    * server-side grid is rendered in different way.
    *
    * @param mgrid    Overall grid state.
    * @param children Children tags.
    * @return Rendered grid VDOM.
    */
  def apply(mgrid: MGridCoreS)
           (children: Iterator[(GridAdKey_t, TagOf[dom.html.Element])]): VdomElement


  /** Should children-renderer prefer use a-tag container instead of div or others? */
  def preferLinkContainer: Boolean

}
