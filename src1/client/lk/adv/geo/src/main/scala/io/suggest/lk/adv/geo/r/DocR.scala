package io.suggest.lk.adv.geo.r

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.geo.m.{DocReadMoreClick, MDocS}
import io.suggest.sjs.common.i18n.Messages
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

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

    def render(mDocProxy: Props): ReactElement = {
      val expanded = mDocProxy().expanded

      <.div(

        expanded ?= {
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


  val component = ReactComponentB[Props]("Doc")
    .stateless
    .renderBackend[Backend]
    .build

  def apply( mDocProxy: Props ) = component( mDocProxy )

}
