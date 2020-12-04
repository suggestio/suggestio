package io.suggest.sc.v.dia.settings

import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.os.notify.{NotificationPermAsk, NotifyStartStop}
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

  private lazy val innerComp = onOffSettingR.prepare(
    text = crCtxProv.message( MsgCodes.`Notifications` ),
    onOffAction = { isEnabled =>
      if (isEnabled)
        NotificationPermAsk( isVisible = true )
      else
        NotifyStartStop( isStart = isEnabled )
    },
  )

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsProxy =>
      propsProxy.wrap(_.hasPermission)( innerComp.component.apply )
    }
    .build

}
