package io.suggest.sc.v.menu

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.ble.beaconer.m.BbOnOff
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.Implicits._
import scalacss.ScalaCssReact._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.06.18 19:18
  * Description: Класс для компонента bluetooth-подменюшки, которая управляет
  * bluetooth-подсистемой, отображает её состояние и возможную справочную информацию.
  */
class BlueToothR(
                  getScCssF  : GetScCssF,
                ) {

  type Props_t = Pot[Boolean]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клики по bluetooth. */
    private def _btOnOffClick(isEnable: Boolean): Callback = {
      dispatchOnProxyScopeCB( $, BbOnOff(isEnable, hard = true) )
    }

    def render(propsProxy: Props): VdomElement = {
      val isEnabledPot = propsProxy.value
      isEnabledPot.renderEl { isEnabledNow =>
        // Доступно API для bluetooth.
        val scCss = getScCssF()
        val menuRowsCss = scCss.Menu.Rows

        <.div(
          menuRowsCss.rowOuter,

          <.a(
            menuRowsCss.rowLink,

            // Кликать можно только, когда не pending и есть какое-то реальное состояние.
            ReactCommonUtil.maybe(!isEnabledPot.isPending) {
              TagMod(
                ^.onClick --> _btOnOffClick( !isEnabledNow ),
                ^.`class` := Css.CLICKABLE
              )
            },

            <.div(
              menuRowsCss.rowContent,

              // Не переводим на другие языки - всегда латиница.
              MsgCodes.`Bluetooth`,

              <.span(
                ^.`class` := Css.Floatt.RIGHT,

                if (isEnabledPot.isPending) {
                  // LkPreLoader тут недоступен, поэтому просто многоточие пока рисуем:
                  TagMod(
                    ^.title := Messages( MsgCodes.`Please.wait` ),
                    HtmlConstants.ELLIPSIS
                  )
                } else if (isEnabledPot.isFailed) {
                  isEnabledPot.exceptionOption.whenDefined { ex =>
                    TagMod(
                      ^.title := ex.toString,
                      ^.`class` := Css.Text.BOLD,
                      Messages( MsgCodes.`Error` )
                    )
                  }
                } else {
                  Messages( MsgCodes.onOff(isEnabledNow) )
                }
              )
            )

          )

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValOptProxy: Props) = component( propsValOptProxy )

}
