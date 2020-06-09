package io.suggest.css

import diode.react.ModelProxy
import io.suggest.css.ScalaCssDefaults._
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 14:38
  * Description: Компонент абстрактого рендера любого css stylesheet'а scalaCSS.
  */
object CssR {

  type Props = StyleSheet.Base

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { stylSheet =>
      <.styleTag(
        // iOS 13 safari на iphone5 косячит с js на scala-2.13, неправильно отрабатывает sci.HashMap.
        // Что-то из серии https://github.com/scala-js/scala-js/issues/2895
        stylSheet.render[String]
      )
    }
    .build

  lazy val compProxied = component.cmapCtorProps[ModelProxy[_ <: StyleSheet.Base]]( _.value )

}
