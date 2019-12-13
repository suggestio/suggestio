package io.suggest.sc.v.menu

import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.sc.m.GeoLocOnOff
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ScalaComponent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.18 18:20
  * Description: Компонент item'а управления геолокацией.
  */
class GeoLocR(
               slideMenuItemR: SlideMenuItemR
             ) {

  type Props_t = Pot[Boolean]
  type Props = ModelProxy[Props_t]


  private lazy val _onOffClickAction = {
    onOff: Boolean =>
      // TODO выставлять как hard, когда будет возможность. Чтобы не сбивалось управление через автоматику.
      GeoLocOnOff( enabled = onOff, isHard = true )
  }


  val OnOffR = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { propsPotProxy =>
      propsPotProxy.wrap { propsPot =>
        for (isEnabled <- propsPot) yield {
          slideMenuItemR.PropsVal(
            isEnabled   = isEnabled,
            text        = MsgCodes.`Geolocation`,
            useMessages = true,
            onOffAction = _onOffClickAction
          )
        }
      }( slideMenuItemR.apply )(implicitly, FastEq.AnyRefEq)
    }
    .build

}
