package io.suggest.css

import diode.react.ModelProxy
import io.suggest.log.Log
import io.suggest.sc.styl.ScScalaCssDefaults._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
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
      val p = props.value

      <.styleTag(
        // iOS 13 safari косячит с js на scala-2.13, неправильно отрабатывает sci.HashMap.
        // Что-то из серии https://github.com/scala-js/scala-js/issues/2895
        props.value
          .render[String]
      )
    }
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .componentDidCatch { $ =>
      Callback.empty
    }
    .build

  def apply(cssProxy: Props) = component( cssProxy )

}
