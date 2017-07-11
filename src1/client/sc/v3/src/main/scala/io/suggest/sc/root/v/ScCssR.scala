package io.suggest.sc.root.v

import diode.react.ModelProxy
import io.suggest.sc.styl.ScScalaCssDefaults._
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.styl.ScCss.scCss
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 16:09
  * Description: React-компонент, рендерящий динамический css выдачи.
  */
object ScCssR {

  type PropsVal = Option[MColors]
  type Props = ModelProxy[PropsVal]

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      <.styleTag(
        scCss.render[String]
      )
    }
  }


  val component = ScalaComponent.builder[Props]("ScCss")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(colorsProxy: Props) = component( colorsProxy )

}
