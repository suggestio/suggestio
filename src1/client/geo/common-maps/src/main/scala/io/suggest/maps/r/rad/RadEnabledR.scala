package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.{MRad, RadOnOff}
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent, ReactEventFromInput}

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

    def onRadEnabledChanged(e: ReactEventFromInput): Callback = {
      val isEnabled = e.target.checked
      dispatchOnProxyScopeCB( $, RadOnOff(isEnabled) )
    }

    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy()
      val hint = Messages( MsgCodes.`Adv.on.map.hint` )

      <.label(
        ^.`class` := Css.CLICKABLE,

        (^.title := hint)
          .unless( p.renderHintAsText ),

        <.input(
          ^.`type`    := HtmlConstants.Input.checkbox,
          ^.checked   := p.enabled,
          ^.onChange ==> onRadEnabledChanged
        ),

        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),

        Messages( MsgCodes.`Adv.on.map` ),
        <.span(
          <.br,
          hint
        )
          .when( p.renderHintAsText )
      )
    }

  }

  val component = ScalaComponent.builder[Props]("RadEnabled")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
