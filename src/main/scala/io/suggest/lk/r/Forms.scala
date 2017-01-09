package io.suggest.lk.r

import io.suggest.css.Css
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactComponentB
import org.scalajs.dom.raw.HTMLDivElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 20:30
  * Description: React-утиль для форм.
  */


/** Контейнер для рендера видимых инпутов в ЛК. */
object Forms {

  val InputCont = ReactComponentB[String]("InputCont")
    .stateless
    .renderPC { (_, props, children) =>
      <.div(
        ^.`class` := (Css.Input.INPUT :: props :: Nil).mkString(" "),
        children
      )
    }
    .domType[HTMLDivElement]
    .propsDefault( Css.Size.S )
    .build

}
