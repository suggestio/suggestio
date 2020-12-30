package io.suggest.sc.v.dia.settings

import com.materialui.{Mui, MuiColorTypes, MuiListItem, MuiListItemProps, MuiListItemText, MuiSwitchProps, MuiToolTip, MuiToolTipProps}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil
import ReactCommonUtil.Implicits._
import io.suggest.lk.r.plat.PlatformComponents
import io.suggest.sc.m.SettingSet
import io.suggest.spa.DAction
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import play.api.libs.json.JsBoolean

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.2020 11:49
  * Description: Строка настройки с одним переключателем.
  */
class OnOffSettingR(
                     crCtxP                 : React.Context[MCommonReactCtx],
                     scComponents           : PlatformComponents,
                   ) {

  case class PropsVal(
                       isCheckedProxy   : ModelProxy[Pot[Boolean]],
                       text             : VdomNode,
                       onOff            : Either[Boolean => DAction, String],
                     )

  type Props = PropsVal

  case class State(
                    isEnabledPotC   : ReactConnectProxy[Pot[Boolean]],
                  )

  private lazy val errorMsg = crCtxP.message( MsgCodes.`Error` )


  class Backend($: BackendScope[Props, State]) {

    private val _onSwitchClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      $.props >>= { props: Props =>
        val isEnabled2 = props.isCheckedProxy
          .value
          .fold(true)(!_)

        val action = props.onOff.fold [DAction] (
          _(isEnabled2),
          confKey => SettingSet( confKey, JsBoolean(isEnabled2), save = true )
        )

        props.isCheckedProxy.dispatchCB( action )
      }
    }

    def render(p: Props, s: State): VdomElement = {
      MuiListItem {
        new MuiListItemProps {
          override val onClick = _onSwitchClickCbF
          override val button = true
        }
      } (

        MuiListItemText()(
          p.text,
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
                  override val title = msg.rawNode
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
        isEnabledPotC = propsProxy.isCheckedProxy.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
