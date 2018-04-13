package io.suggest.adn.edit.v

import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.PropTableR
import io.suggest.msg.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 18:08
  * Description: Компонент контейнера
  */
class WcFgContR {

  val component = ScalaComponent.builder[Unit]("WcFgCont")
    .stateless
    .render_C { children =>
      <.div(
        ^.`class` := Css.PropTable.TABLE,
        <.p(
          ^.`class` := Css.flat( Css.PropTable.TD_NAME, Css.PropTable.BLOCK ),
          Messages( MsgCodes.`Welcome.screen` ),
          HtmlConstants.`COLON`
        ),
        PropTableR.Outer(
          <.tr(
            <.td(
              ^.`class` := Css.flat( Css.PropTable.TD_NAME, Css.Table.Td.TD ),
              Messages( MsgCodes.`Welcome.bg.hint` )
            ),
            <.td(
              ^.`class` := Css.Table.Td.TD,
              children
            )
          )
        )
      )
    }
    .build

  def apply(children: VdomNode*) = component(children: _*)

}
