package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.sc.m.GeoLocOnOff
import io.suggest.sc.m.dev.MGeoLocSwitchS
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.2020 21:28
  * Description: Настройка геолокации.
  */
class GeoLocSettingR(
                      onOffSettingR   : OnOffSettingR,
                      crCtxProv       : React.Context[MCommonReactCtx],
                    ) {

  type Props_t = MGeoLocSwitchS
  type Props = ModelProxy[Props_t]


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsProxy =>
      val innerComp = onOffSettingR.prepare(
        text = crCtxProv.message( MsgCodes.`Geolocation` ),
        onOffAction = { isEnabled =>
          GeoLocOnOff(
            enabled = isEnabled,
            isHard  = true,
          )
        },
      )
      propsProxy.wrap(_.onOff)( innerComp.component.apply )
    }
    .build

}
