package io.suggest.sc.v.hdr

import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 18:13
  * Description: Вывод названия узла в рамочке.
  */
class NodeNameR( getScCssF: GetScCssF ) {

  type Props_t = Option[String]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(nodeNameOptProxy: Props): VdomElement = {
      nodeNameOptProxy().whenDefinedEl { nodeName =>
        // Отрендерить название текущего узла.
        val scCss = getScCssF()
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


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  private def _apply(nodeNameOptProxy: Props) = component( nodeNameOptProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
