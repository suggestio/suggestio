package io.suggest.sc.v.dia.settings

import com.materialui.{MuiLink, MuiLinkProps, MuiListItem, MuiListItemText, MuiPaper}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.dev.{MOsFamily, MPlatformS}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import io.suggest.sc.m.menu.DlAppOpen
import io.suggest.sc.m.SettingsDiaOpen
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.2020 18:34
  * Description: wrap-компонент плашки в настройках о недоступности bluetooth.
  */
class BlueToothUnAvailInfoR(
                             crCtxProv               : React.Context[MCommonReactCtx],
                           ) {

  type Props_t = MPlatformS
  type Props = ModelProxy[Props_t]

  case class State(
                    btMissOnOsC       : ReactConnectProxy[Option[Option[MOsFamily]]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onInstallAppClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SettingsDiaOpen(opened = false) ) >>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DlAppOpen(opened = true) )
    }

    def render(s: State): VdomElement = {
      // Если bluetooth недоступен на данной платформе, то вывести сообщение:
      s.btMissOnOsC { btMissOnOsProxy =>
        btMissOnOsProxy.value.whenDefinedEl { btMissOpt =>
          crCtxProv.consume { crCtx =>
            MuiListItem()(
              MuiPaper()(

                MuiListItemText()(
                  crCtx.messages(
                    MsgCodes.`0.not.supported.on.this.platform.1`,
                    MsgCodes.`Bluetooth`,
                    btMissOpt.fold( crCtx.messages(MsgCodes.`Browser`) )( _.value ),
                  ),
                ),

                MuiListItemText()(
                  MuiLink(
                    new MuiLinkProps {
                      override val onClick = _onInstallAppClickCbF
                    }
                  )(
                    crCtx.messages( MsgCodes.`Install.app.for.access.to.0`, MsgCodes.`Bluetooth` )
                  ),
                ),

              ),
            )
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        btMissOnOsC = propsProxy.connect { p =>
          Option.when( !p.hasBle )( p.osFamily )
        },
      )
    }
    .renderBackend[Backend]
    .build

}
