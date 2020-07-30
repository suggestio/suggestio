package io.suggest.sc.v.dia.settings

import com.materialui.{Mui, MuiColorTypes, MuiListItem, MuiListItemProps, MuiListItemText, MuiSwitchProps, MuiToolTip, MuiToolTipProps}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.sc.v.styl.ScComponents
import io.suggest.spa.DAction
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.2020 11:49
  * Description: Строка настройки с одним переключателем.
  */
class OnOffSettingR(
                     crCtxP                 : React.Context[MCommonReactCtx],
                     scComponents           : ScComponents,
                   ) {

  type Props_t = Pot[Boolean]
  type Props = ModelProxy[Props_t]

  case class State(
                    isEnabledPotC   : ReactConnectProxy[Pot[Boolean]],
                  )


  /** Статические части компонента передаются через конструктор промежуточного класса. */
  case class prepare(
                      text           : VdomNode,
                      onOffAction    : Boolean => DAction,
                    ) {

    class Backend($: BackendScope[Props, State]) {

      private val _onSwitchClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
        ReactDiodeUtil.dispatchOnProxyScopeCBf( $ ) { propsProxy: Props =>
          onOffAction( propsProxy.value.fold(true)(!_) )
        }
      }

      def render(s: State): VdomElement = {
        lazy val errorMsg = crCtxP.message( MsgCodes.`Error` )

        MuiListItem {
          new MuiListItemProps {
            override val onClick = _onSwitchClickCbF
            override val button = true
          }
        } (
          MuiListItemText()(
            text,
          ),

          s.isEnabledPotC { isEnabledPotProxy =>
            val pot = isEnabledPotProxy.value

            React.Fragment(
              // Рендер инфы о ошибке:
              pot.renderFailed { ex =>
                val msg = <.span(
                  errorMsg,
                  HtmlConstants.COLON,
                  HtmlConstants.SPACE,
                  ex.getMessage,
                )

                MuiToolTip {
                  new MuiToolTipProps {
                    override val title: raw.React.Node = msg.rawNode
                  }
                } (
                  Mui.SvgIcons.Warning()(),
                )
              },

              scComponents.muiSwitch {
                val _isChecked = pot getOrElse false
                new MuiSwitchProps {
                  override val checked = js.defined( _isChecked )
                  override val disabled = pot.isPending
                  // Есть какая-то проблема со switch: он внезапно может менять дизайн, становясь частично белым на белом фоне диалога.
                  override val color = MuiColorTypes.secondary
                }
              },
            )
          },

        )
      }

    }


    val component = ScalaComponent
      .builder[Props]( getClass.getSimpleName )
      .initialStateFromProps { propsProxy =>
        State(
          isEnabledPotC = propsProxy.connect(identity),
        )
      }
      .renderBackend[Backend]
      .build

  }

}
