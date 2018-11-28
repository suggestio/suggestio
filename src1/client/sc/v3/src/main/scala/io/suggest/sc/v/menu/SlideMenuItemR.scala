package io.suggest.sc.v.menu

import chandu0101.scalajs.react.components.materialui.{Mui, MuiListItem, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps, MuiSwitch, MuiSwitchClasses, MuiSwitchProps, MuiToolTip, MuiToolTipProps}
import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
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
import io.suggest.spa.DAction
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.18 15:39
  * Description: React-компонент пункта меню для управление геолокацией.
  */
class SlideMenuItemR(
                      getScCssF  : GetScCssF,
                    ) {

  /** Модель данных для рендера пункта меню со слайдером. */
  case class PropsVal(
                       isEnabled      : Boolean,
                       text           : String,
                       useMessages    : Boolean,
                       onOffAction    : Boolean => DAction,
                     )
  implicit object SlideItemRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.isEnabled ==* b.isEnabled) &&
      (a.text ===* b.text) &&
      (a.useMessages ==* b.useMessages) &&
      (a.onOffAction ===* b.onOffAction)
    }
  }


  type Props_t = Pot[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на переключение состояния.
      * @param isEnable Новое состояние on/off, желаемое пользователем.
      */
    private def _btOnOffClick(isEnable: Boolean)(e: ReactEvent): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsPotProxy: Props =>
        propsPotProxy.value.get.onOffAction( isEnable )
      }
    }


    def render(propsPotProxy: Props): VdomElement = {
      val propsPot = propsPotProxy.value
      propsPot.renderEl { props =>
        // Доступно API для bluetooth.
        val scCss = getScCssF()
        val menuRowsCss = scCss.Menu.Rows
        val isClickDisabled = propsPot.isPending

        // Ссылка на вход или на личный кабинет
        MuiListItem {
          val _onClickF = JsOptionUtil.maybeDefined(!isClickDisabled) {
            ReactCommonUtil.cbFun1ToJsCb( _btOnOffClick(!props.isEnabled) )
          }
          new MuiListItemProps {
            override val disableGutters = true
            override val button = !isClickDisabled
            override val onClick = _onClickF
          }
        } (
          MuiListItemText {
            val cssClasses = new MuiListItemTextClasses {
              override val root = menuRowsCss.rowText.htmlClass
            }
            new MuiListItemTextProps {
              override val classes = cssClasses
            }
          } (

            <.span(
              menuRowsCss.rowContent,
              if (props.useMessages) Messages( props.text )
              else props.text
            ),

            <.span(
              ^.`class` := Css.Floatt.RIGHT,

              // Рендер инфы о ошибке:
              propsPot.exceptionOption.whenDefined { ex =>
                val msg = Messages( MsgCodes.`Error` ) +
                  HtmlConstants.COLON +
                  HtmlConstants.SPACE +
                  ex.getMessage

                MuiToolTip {
                  new MuiToolTipProps {
                    override val title: raw.React.Node = msg
                  }
                }(
                  Mui.SvgIcons.Warning()()
                )
              },

              // Рендер самого переключателя:
              MuiToolTip {
                new MuiToolTipProps {
                  override val title: raw.React.Node = Messages( MsgCodes.onOff(props.isEnabled) )
                }
              }(
                MuiSwitch {
                  val _isChecked =
                    if (isClickDisabled) !props.isEnabled
                    else props.isEnabled
                  val cssClasses = new MuiSwitchClasses {
                    override val root = menuRowsCss.switch.htmlClass
                    override val switchBase = menuRowsCss.switchBase.htmlClass
                  }
                  new MuiSwitchProps {
                    override val disabled = isClickDisabled
                    override val checked  = js.defined( _isChecked )
                    override val classes  = cssClasses
                  }
                }
              )

            )

          )
        )

      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValOptProxy: Props) = component( propsValOptProxy )

}