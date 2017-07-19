package io.suggest.sc.hdr.v

import diode.react.ModelProxy
import io.suggest.sc.styl.ScCss.scCss
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 18:13
  * Description: Вывод названия узла в рамочке.
  */
object NodeNameR {

  type Props = ModelProxy[Option[String]]

  class Backend($: BackendScope[Props, Unit]) {

    def render(nodeNameOptProxy: Props): VdomElement = {
      nodeNameOptProxy().whenDefinedEl { nodeName =>
        // Отрендерить название текущего узла.
        val styles = scCss.Header.Logo.Txt
        val dotsStyles = styles.Dots
        <.span(
          styles.txtLogo,
          nodeName,
          <.span( dotsStyles.dot, dotsStyles.left ),
          <.span( dotsStyles.dot, dotsStyles.right )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("NodeName")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodeNameOptProxy: Props) = component( nodeNameOptProxy )

}
