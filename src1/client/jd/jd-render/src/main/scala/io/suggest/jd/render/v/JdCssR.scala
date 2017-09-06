package io.suggest.jd.render.v

import diode.react.ModelProxy
import io.suggest.css.ScalaCssDefaults._
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 20:51
  * Description: React-компонент для рендера JdCss в style-тэг.
  */
class JdCssR( jdCssFactory: JdCssFactory ) {

  type Props = ModelProxy[JdCss]

  val component = ScalaComponent.builder[Props]("JdCss")
    .stateless
    .render_P { propsProxy =>
      // Отрендерить style-тег:
      <.styleTag(
        propsProxy.value.render[String]
      )
    }
    .build

  def apply(jdCssArgsProxy: Props) = component( jdCssArgsProxy )

}
