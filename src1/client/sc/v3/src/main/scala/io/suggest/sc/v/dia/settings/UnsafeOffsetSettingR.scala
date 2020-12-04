package io.suggest.sc.v.dia.settings

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiListItem, MuiListItemProps, MuiListItemText, MuiListItemTextProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.dev.{MScreen, MScreenInfo, MTlbr}
import io.suggest.i18n.MsgCodes
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.UpdateUnsafeScreenOffsetBy
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React.Node
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js.UndefOr
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.06.18 10:36
  * Description: Отладочный компонент для ручного выставления сдвига безопасной области экрана в пикселях.
  */
class UnsafeOffsetSettingR {

  type Props_t = MScreenInfo
  type Props = ModelProxy[Props_t]


  case class State(
                    mtbrOptC      : ReactConnectProxy[MTlbr],
                    screenC       : ReactConnectProxy[MScreen],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onIncDecClick(incDecBy: Int): Callback =
      dispatchOnProxyScopeCB( $, UpdateUnsafeScreenOffsetBy(incDecBy) )

    private def _mkPlusMinusBtn(diff: Int)(content: VdomNode): VdomElement = {
      MuiButton(
        new MuiButtonProps {
          override val onClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => _onIncDecClick(diff) }
          override val size = MuiButtonSizes.small
          override val variant = MuiButtonVariants.outlined
        }
      )(
        <.strong(
          content
        )
      )
    }

    def render(s: State): VdomElement = {
      // Ссылка на вход или на личный кабинет
      val liProps = new MuiListItemProps {
        override val button = false
      }

      React.Fragment(



        // Строка с контролом для замены offset'ов
        MuiListItem(
          liProps
        )(
          MuiListItemText()(
            <.span(
              MsgCodes.`Unsafe.offset`,

              <.span(
                ^.`class` := Css.Floatt.RIGHT,

                // Текущее значение:
                s.mtbrOptC { mtbrOptProxy =>
                  <.span(
                    mtbrOptProxy.value.toString,
                  )
                },

                <.br,

                // Уменьшение
                _mkPlusMinusBtn(-1)( MINUS ),
                SPACE,
                // Увеличение
                _mkPlusMinusBtn(+1)( PLUS ),

              )
            )
          )
        ),

        // Показать текущие параметры экрана:
        MuiListItem(
          liProps
        )(
          MuiListItemText()(
            <.span(
              MsgCodes.`Screen`,

              <.span(
                ^.`class` := Css.Floatt.RIGHT,

                s.screenC { screenProxy =>
                  val screen = screenProxy.value
                  <.strong(
                    screen.wh.width,
                    "x",
                    screen.wh.height,
                    "@",
                    screen.pxRatio.value,
                  )
                },
              )
            )
          )
        ),

        // Рендер строки User-Agent
        MuiListItem(
          liProps
        )(
          MuiListItemText {
            val uaStr = Try( dom.window.navigator.userAgent )
              .fold(_.toString, identity)
            new MuiListItemTextProps {
              override val primary = "User-Agent"
              override val secondary = uaStr
            }
          }()
        ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        mtbrOptC = propsProxy.connect( _.unsafeOffsets ),
        screenC = propsProxy.connect( _.screen ),
      )
    }
    .renderBackend[Backend]
    .build

}
