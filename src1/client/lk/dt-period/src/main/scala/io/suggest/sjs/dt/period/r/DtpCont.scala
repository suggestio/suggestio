package io.suggest.sjs.dt.period.r

import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.css.Css
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.{ReactComponentB, ReactNode}

/** Компонент внешнего контейнера виджета. */
object DtpCont {

  val component = ReactComponentB[Unit]("DtpCont")
    .stateless
    .render_C { children =>
      // Контейнер виджета
      <.div(
        ^.`class` := Css.Lk.Adv.RIGHT_BAR,

        // Заголовок виджета
        <.h2(
          ^.`class` := Css.Lk.MINOR_TITLE,
          Messages( "Date.choosing" )
        ),

        // Контейнер кусков виджета.
        <.div(
          ^.`class` := Css.Dt.DT_WIDGET,
          children
        )
      )
    }
    .build

  def apply(children: ReactNode*) = component(children: _*)

}
