package io.suggest.css

import diode.react.ModelProxy
import io.suggest.sc.styl.ScScalaCssDefaults._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 14:38
  * Description: Компонент абстрактого рендера любого css stylesheet'а scalaCSS.
  */
object CssR {

  type Props = ModelProxy[_ <: StyleSheet.Base]

  class Backend($: BackendScope[Props, Unit]) {
    def render(props: Props): VdomElement = {
      <.styleTag(
        props.value.render[String]
      )
    }
  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(cssProxy: Props) = component( cssProxy )

}
