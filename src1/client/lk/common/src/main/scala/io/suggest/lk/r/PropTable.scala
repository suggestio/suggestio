package io.suggest.lk.r

import io.suggest.css.Css
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 18:42
  * Description: React-компоненты таблицы prop table.
  *
  * {{{
  *   PropTable.Outer(
  *     PropTable.Row(
  *       "test",
  *       someValueOfAnyType...
  *     ),
  *     PropTable.Row(...),
  *     ...
  *   )
  * }}}
  */
object PropTable {

  val Outer = ScalaComponent.builder[Unit]("PropTable")
    .stateless
    .render_C { children =>
      <.table(
        ^.`class` := Css.PropTable.TABLE,
        <.tbody(
          children
        )
      )
    }
    .build


  /** Один ряд таблицы. */
  val Row = ScalaComponent.builder[String]("PropTableRow")
    .stateless
    .renderPC { (_, name, children) =>
      <.tr(
        <.td(
          ^.`class` := Css.PropTable.TD_NAME,
          name
        ),
        <.td(
          ^.`class` := Css.PropTable.TD_VALUE,
          children
        )
      )
    }
    .build

}
