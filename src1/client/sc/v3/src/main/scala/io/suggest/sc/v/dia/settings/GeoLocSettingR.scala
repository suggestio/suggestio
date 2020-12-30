package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.conf.ConfConst
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
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
      onOffSettingR.component(
        onOffSettingR.PropsVal(
          text            = crCtxProv.message( MsgCodes.`Geolocation` ),
          onOff           = Right( ConfConst.ScSettings.LOCATION_ENABLED ),
          isCheckedProxy  = propsProxy.zoom(_.onOff),
        )
      )
    }
    .build

}
