package io.suggest.sc.v

import diode.react.ModelProxy
import io.suggest.sc.styl.ScScalaCssDefaults._
import io.suggest.sc.styl.{GetScCssF, MScCssArgs}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 16:09
  * Description: React-компонент, рендерящий динамический css выдачи.
  */
class ScCssR( getScCssF: GetScCssF ) {

  type Props = ModelProxy[MScCssArgs]

  class Backend($: BackendScope[Props, Unit]) {
    def render: VdomElement = {
      val scCss = getScCssF()

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
