package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.ble.beaconer.{BtOnOff, MBeaconerOpts, MBeaconerS}
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

  private lazy val innerComp = onOffSettingR.prepare(
    text = MsgCodes.`Bluetooth`,
    onOffAction = { isEnabled =>
      BtOnOff(
        isEnabled = isEnabled,
        opts = MBeaconerOpts(
          hardOff = true,
          askEnableBt = true,
          oneShot = false,
        ),
      )
    },
  )

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsProxy =>
      propsProxy.wrap( _.isEnabled )( innerComp.component.apply )
    }
    .build

}
