package io.suggest.sc.v.menu

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.dev.MTlbr
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.UpdateUnsafeScreenOffsetBy
import io.suggest.sc.styl.GetScCssF
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.06.18 10:36
  * Description: Отладочный компонент для ручного выставления сдвига безопасной области экрана в пикселях.
  */
class UnsafeScreenAreaOffsetR(
                               getScCssF  : GetScCssF
                             ) {

  type Props_t = Option[MTlbr]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private def _onIncDecClick(incDecBy: Int): Callback = {
      dispatchOnProxyScopeCB( $, UpdateUnsafeScreenOffsetBy(incDecBy) )
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { mtlbr =>
        import HtmlConstants._

        val menuRowsCss = getScCssF().Menu.Rows

        <.div(
          menuRowsCss.rowOuter,

          <.div(
            menuRowsCss.rowContent,
            "Unsafe offset",

            <.span(
              ^.`class` := Css.Floatt.RIGHT,

              // Уменьшение
              <.a(
                ^.onClick --> _onIncDecClick(-1),
                LESSER,
                MINUS
              ),

              NBSP_STR,
              mtlbr.toString,
              NBSP_STR,

              // Увеличение
              <.a(
                ^.onClick --> _onIncDecClick(1),
                PLUS,
                GREAGER
              )
            )

          )

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mtlbrOptProxy: Props) = component( mtlbrOptProxy )

}
