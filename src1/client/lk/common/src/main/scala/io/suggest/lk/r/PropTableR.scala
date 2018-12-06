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
object PropTableR {

  val Outer = ScalaComponent.builder[Unit](getClass.getSimpleName)
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

  def Name(name: TagMod) = _nameOrValue(Css.PropTable.TD_NAME, name)
  def Value(value: TagMod) = _nameOrValue( Css.PropTable.TD_VALUE, value )

  private def _nameOrValue(cssClass: String, content: TagMod): VdomElement = {
    <.td(
      ^.`class` := cssClass,
      content
    )
  }

  /** Один ряд таблицы. */
  val Row = ScalaComponent.builder[TagMod](getClass.getSimpleName + "Row")
    .stateless
    .renderPC { (_, name, children) =>
      <.tr(
        Name( name ),
        Value( children )
      )
    }
    .build

}
