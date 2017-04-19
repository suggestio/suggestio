package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.{MRad, RadOnOff}
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}

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
      dispatchOnProxyScopeCB( $, RadOnOff(isEnabled) )
    }

    def render(propsProxy: Props): ReactElement = {
      val p = propsProxy()
      val hint = Messages( MsgCodes.`Adv.on.map.hint` )

      <.label(
        ^.`class` := Css.CLICKABLE,

        !p.renderHintAsText ?= {
          ^.title := hint
        },

        <.input(
          ^.`type`    := HtmlConstants.Input.checkbox,
          ^.checked   := p.enabled,
          ^.onChange ==> onRadEnabledChanged
        ),

        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),

        Messages( MsgCodes.`Adv.on.map` ),
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
