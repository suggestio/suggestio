package io.suggest.lk.adv.geo.r.geo.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.lk.adv.geo.a.RadOnOff
import io.suggest.lk.adv.geo.m.MRad
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.vm.LkMessagesWindow.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.17 16:38
  * Description: React-компонент галочки активности размещения на карте в радиусе.
  */
object RadEnabledR {

  type Props = ModelProxy[PropsVal]

  case class PropsVal(enabled: Boolean, renderHintAsText: Boolean)

  /** Сборка коннекшена до enabled-props. */
  def radEnabledPropsConn(mradOptProxy: ModelProxy[Option[MRad]], renderHintAsText: Boolean): ReactConnectProxy[RadEnabledR.PropsVal] = {
    mradOptProxy.connect { mradOpt =>
      RadEnabledR.PropsVal(
        enabled             = mradOpt.exists(_.enabled),
        renderHintAsText    = renderHintAsText
      )
    }
  }


  protected class Backend($: BackendScope[Props, Unit]) {

    def onRadEnabledChanged(e: ReactEventI): Callback = {
      val isEnabled = e.target.checked
      $.props >>= { p =>
        p.dispatchCB( RadOnOff(isEnabled) )
      }
    }

    def render(propsProxy: Props): ReactElement = {
      val p = propsProxy()
      val hint = Messages("Adv.on.map.hint")

      <.label(
        !p.renderHintAsText ?= {
          ^.title := hint
        },

        <.input(
          ^.`type`    := "checkbox",
          ^.checked   := p.enabled,
          ^.onChange ==> onRadEnabledChanged
        ),

        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),

        Messages( "Adv.on.map" ),
        p.renderHintAsText ?= {
          <.span(
            <.br,
            hint
          )
        }
      )
    }

  }

  val component = ReactComponentB[Props]("RadEnabled")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
