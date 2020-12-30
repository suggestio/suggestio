package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.ble.beaconer.{BtOnOff, MBeaconerS}
import io.suggest.conf.ConfConst
import io.suggest.i18n.MsgCodes
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.2020 21:28
  * Description: Настройка геолокации.
  */
class BlueToothSettingR(
                         onOffSettingR   : OnOffSettingR,
                       ) {

  type Props_t = MBeaconerS
  type Props = ModelProxy[Props_t]

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsProxy =>
      onOffSettingR.component(
        onOffSettingR.PropsVal(
          text            = MsgCodes.`Bluetooth`,
          onOff           = Right( ConfConst.ScSettings.BLUETOOTH_BEACONS_ENABLED ),
          isCheckedProxy  = propsProxy.zoom(_.isEnabled),
        )
      )
    }
    .build

}
