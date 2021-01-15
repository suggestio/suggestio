package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.ble.beaconer.MBeaconerS
import io.suggest.conf.ConfConst
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.2020 21:28
  * Description: Настройка геолокации.
  */
class BlueToothSettingR(
                         onOffSettingR          : OnOffSettingR,
                         crCtxP                 : React.Context[MCommonReactCtx],
                       ) {

  type Props_t = MBeaconerS
  type Props = ModelProxy[Props_t]

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsProxy =>
      val S = ConfConst.ScSettings
      <.div(
        onOffSettingR.component(
          onOffSettingR.PropsVal(
            text            = MsgCodes.`Bluetooth`,
            onOff           = Right( S.BLUETOOTH_BEACONS_ENABLED ),
            isCheckedProxy  = propsProxy.zoom(_.isEnabled),
          )
        ),

        /*onOffSettingR.component(
          onOffSettingR.PropsVal(
            text = crCtxP.message( MsgCodes.`Background.mode` ),
            onOff = Right( S.BLUETOOTH_BEACONS_BACKGROUND_SCAN ),
          )
        ),*/

      )
    }
    .build

}
