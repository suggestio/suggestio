package io.suggest.lk.r

import io.suggest.css.Css
import japgolly.scalajs.react.{ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.{HTMLTableElement, HTMLTableRowElement}


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

  object Outer {

    val component = ReactComponentB[Unit]("PropTable")
      .stateless
      .render_C { children =>
        <.table(
          ^.`class` := Css.PropTable.TABLE,
          <.tbody(
            children
          )
        )
      }
      .domType[HTMLTableElement]
      .build

    def apply(children: ReactElement*)     = component(children)
    //def apply(children: Seq[ReactElement]) = component(children)

  }


  /** Один ряд таблицы. */
  val Row = ReactComponentB[String]("PropTableRow")
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
    .domType[HTMLTableRowElement]
    .build

}
