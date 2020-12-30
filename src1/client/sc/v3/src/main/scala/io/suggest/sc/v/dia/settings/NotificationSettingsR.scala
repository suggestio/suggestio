package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.conf.ConfConst
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.sc.m.dev.MScOsNotifyS
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.2020 21:32
  * Description:
  */
class NotificationSettingsR(
                             onOffSettingR   : OnOffSettingR,
                             crCtxProv       : React.Context[MCommonReactCtx],
                           ) {

  type Props_t = MScOsNotifyS
  type Props = ModelProxy[Props_t]


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsProxy =>
      onOffSettingR.component(
        onOffSettingR.PropsVal(
          text            = crCtxProv.message( MsgCodes.`Notifications` ),
          onOff           = Right( ConfConst.ScSettings.LOCATION_ENABLED ),
          isCheckedProxy  = propsProxy.zoom(_.hasPermission),
        )
      )
    }
    .build

}
