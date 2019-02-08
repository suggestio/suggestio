package io.suggest.sc.v.menu

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.ble.beaconer.m.BtOnOff
import io.suggest.i18n.MsgCodes
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ScalaComponent
import io.suggest.spa.FastEqUtil.PotFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.06.18 19:18
  * Description: Класс для компонента bluetooth-подменюшки, которая управляет
  * bluetooth-подсистемой, отображает её состояние и возможную справочную информацию.
  */
class BlueToothR(
                  slideMenuItemR: SlideMenuItemR
                ) {

  import slideMenuItemR.SlideItemRPropsValFastEq

  type Props_t = Pot[Boolean]
  type Props = ModelProxy[Props_t]


  private lazy val _onOffClickAction = {
    onOff: Boolean =>
      BtOnOff( isEnabled = onOff, hard = true )
  }


  val OnOffR = ScalaComponent
    .builder[Props](getClass.getSimpleName )
    .stateless
    .render_P { propsPotProxy =>
      propsPotProxy.wrap { propsPot =>
        for (isEnabled <- propsPot) yield {
          slideMenuItemR.PropsVal(
            isEnabled   = isEnabled,
            text        = MsgCodes.`Bluetooth`,
            useMessages = false,
            onOffAction = _onOffClickAction
          )
        }
      }( slideMenuItemR.apply )(implicitly, PotFastEq(SlideItemRPropsValFastEq))
    }
    .build

}
