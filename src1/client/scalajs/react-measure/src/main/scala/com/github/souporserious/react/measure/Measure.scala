package com.github.souporserious.react.measure

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.08.2019 17:08
  */
object Measure {

  val component = JsComponent[MeasureProps, Children.None, Null]( MeasureJs )

  private def _mkChildrenJsF(f0: Ref.Simple[html.Element] => VdomNode) = {
    f0.compose[RefHandle_t](Ref.fromJs)
      .andThen(_.rawNode: raw.PropsChildren)
  }

  def bounds(onResizeF: Bounds => Unit)(childrenF: Ref.Simple[html.Element] => VdomNode) = {
    val onBoundsF = onResizeF
      .compose[ContentRect](_.bounds.get)
    component(
      new MeasureProps {
        override val bounds   = true
        override val onResize = js.defined( onBoundsF )
        override val children = _mkChildrenJsF( childrenF )
      }
    )
  }

}


@js.native
@JSImport( NPM_PACKAGE_NAME, JSImport.Default )
object MeasureJs extends js.Object


trait MeasureProps extends js.Object {
  val client: js.UndefOr[Boolean] = js.undefined
  val offset: js.UndefOr[Boolean] = js.undefined
  val scroll: js.UndefOr[Boolean] = js.undefined
  val bounds: js.UndefOr[Boolean] = js.undefined
  val margin: js.UndefOr[Boolean] = js.undefined
  val innerRef: js.UndefOr[js.Function1[RefHandle_t, _]] = js.undefined
  val onResize: js.UndefOr[js.Function1[ContentRect, _]] = js.undefined

  /** Children rendering function.
    *
    * For example:
    * {{{
    *   children = { ref =>
    *     <.div.withRef(ref)(
    *       ...
    *     )
    *   }
    * }}}
    */
  val children: js.Function1[RefHandle_t, raw.PropsChildren]
}
