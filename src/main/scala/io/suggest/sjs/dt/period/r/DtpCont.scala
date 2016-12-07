package io.suggest.sjs.dt.period.r

import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.css.Css
import japgolly.scalajs.react.{ReactComponentB, ReactElement}

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

  def apply(children: ReactElement*) = component(children)

}
