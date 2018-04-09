package io.suggest.lk.adv.geo.r

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.geo.m.{DocReadMoreClick, MDocS}
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 22:29
  * Description: Компонент с описанием назначением формы.
  */
object DocR {

  type Props = ModelProxy[MDocS]

  class Backend($: BackendScope[Props, Unit]) {

    private def onReadMoreClick: Callback = {
      dispatchOnProxyScopeCB( $, DocReadMoreClick )
    }

    def render(mDocProxy: Props): VdomElement = {
      val expanded = mDocProxy().expanded

      <.div(

        ReactCommonUtil.maybe( expanded ) {
          ^.`class` := Css.PropTable.TD_VALUE
        },

        Messages( MsgCodes.`Adv.geo.form.descr1` ),
        HtmlConstants.SPACE,

        if ( expanded ) {
          // Рендер более подробной информации.
          <.div(

            <.p(
              Messages( MsgCodes.`User.located.inside.are.or.location.will.see.your.offer` )
            ),

            <.p(
              Messages( MsgCodes.`Good.to.know.that` )
            ),
            <.ul(
              <.li(
                Messages( MsgCodes.`Geo.circle.does.not.touch.any.nodes` )
              ),
              <.li(
                Messages( MsgCodes.`Nodes.can.contain.subnodes.sublocations.routers.tvs.beacons` )
              ),
              <.li(
                Messages( MsgCodes.`Adv.geo.form.descr.price` )
              )
            )

          )

        } else {

          // Рендер ссылки разворачивания подробной документации.
          <.a(
            ^.`class` := Css.flat( Css.Lk.BLUE_LINK, Css.CLICKABLE ),
            ^.onClick --> onReadMoreClick,
            Messages( MsgCodes.`Read.more` )
          )
        },

        <.br

      )
    }

  }


  val component = ScalaComponent.builder[Props]("Doc")
    .stateless
    .renderBackend[Backend]
    .build

  def apply( mDocProxy: Props ) = component( mDocProxy )

}
