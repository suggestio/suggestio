package io.suggest.sc.v.menu

import chandu0101.scalajs.react.components.materialui.{MuiListItem, MuiListItemProps, MuiListItemText, MuiSwitch, MuiSwitchProps, MuiToolTip, MuiToolTipProps}
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.ble.beaconer.m.BbOnOff
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent, raw}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil
import scalacss.ScalaCssReact._
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react.raw.React.Node

import scala.scalajs.js

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

    /** Реакция на клики по bluetooth.
      * @param isEnable Новое состояние bluetooth. Задаётся в вёрстве, чтобы визуально наблюдаемое состояние
      *                 четко отражало намерения пользователя.
      */
    private def _btOnOffClick(isEnable: Boolean)(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, BbOnOff(isEnable, hard = true) )


    def render(propsProxy: Props): VdomElement = {
      val isEnabledPot = propsProxy.value
      isEnabledPot.renderEl { isEnabledNow =>
        // Доступно API для bluetooth.
        val scCss = getScCssF()
        val menuRowsCss = scCss.Menu.Rows
        val isClickDisabled = isEnabledPot.isPending

        // Ссылка на вход или на личный кабинет
        MuiListItem {
          val _onClickF = JsOptionUtil.maybeDefined(!isClickDisabled) {
            ReactCommonUtil.cbFun1ToJsCb( _btOnOffClick(!isEnabledNow) )
          }
          new MuiListItemProps {
            override val disableGutters = true
            override val button = !isClickDisabled
            override val onClick = _onClickF
          }
        } (
          MuiListItemText()(
            <.span(
              menuRowsCss.rowContent,
              MsgCodes.`Bluetooth`
            ),
            MuiToolTip {
              new MuiToolTipProps {
                override val title: raw.React.Node = Messages( MsgCodes.onOff(isEnabledNow) )
              }
            }(
              <.span(
                menuRowsCss.switch,
                MuiSwitch {
                  val _isChecked =
                    if (isClickDisabled) !isEnabledNow
                    else isEnabledNow
                  new MuiSwitchProps {
                    override val disabled = isClickDisabled
                    override val checked = js.defined( _isChecked )
                  }
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
