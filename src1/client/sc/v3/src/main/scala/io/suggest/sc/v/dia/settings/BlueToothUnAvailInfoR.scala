package io.suggest.sc.v.dia.settings

import com.materialui.{MuiLink, MuiLinkClasses, MuiLinkProps, MuiListItem, MuiListItemText, MuiPaper}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.menu.DlAppOpen
import io.suggest.sc.m.SettingsDiaOpen
import io.suggest.sc.m.dev.MScDev
import io.suggest.sc.v.styl.ScCssStatic
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

  type Props_t = MScDev
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private lazy val _onInstallAppClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SettingsDiaOpen(opened = false) ) >>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DlAppOpen(opened = true) )
    }

    def render(p: Props): VdomElement = {
      // Если bluetooth недоступен на данной платформе, то вывести сообщение:
      val hasRadioBeacons = p.value.platform.hasReadioBeacons

      ReactCommonUtil.maybeEl( !hasRadioBeacons ) {
        crCtxProv.consume { crCtx =>
          MuiListItem()(
            MuiPaper()(

              MuiListItemText()(
                crCtx.messages(
                  MsgCodes.`0.not.supported.on.this.platform.1`,
                  MsgCodes.`Bluetooth`,
                  crCtx.messages( MsgCodes.`Browser` ),
                ),
              ),

              // Если это браузер, то вывести плашку о необходимости установки приложения.
              MuiListItemText()(
                MuiLink(
                  new MuiLinkProps {
                    override val onClick = _onInstallAppClickCbF
                    override val classes = new MuiLinkClasses {
                      override val root = ScCssStatic.cursorPointer.htmlClass
                    }
                  }
                )(
                  crCtx.messages( MsgCodes.`Install.app.for.access.to.0`, MsgCodes.`Bluetooth` ),
                ),
              ),

            ),
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
