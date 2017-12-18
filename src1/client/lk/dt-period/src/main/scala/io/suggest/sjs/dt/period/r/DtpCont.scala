package io.suggest.sjs.dt.period.r

import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import japgolly.scalajs.react.ScalaComponent

/** Компонент внешнего контейнера виджета. */
object DtpCont {

  val component = ScalaComponent.builder[Unit]("DtpCont")
    .stateless
    .render_C { children =>
      // Контейнер виджета
      <.div(
        ^.`class` := Css.Lk.Adv.RIGHT_BAR,

        // Заголовок виджета
        <.h2(
          ^.`class` := Css.Lk.MINOR_TITLE,
          Messages( MsgCodes.`Date.choosing` )
        ),

        // Контейнер кусков виджета.
        <.div(
          ^.`class` := Css.Dt.DT_WIDGET,
          children
        )
      )
    }
    .build

  def apply(children: VdomNode*) = component(children: _*)

}
