package io.suggest.lk.r

import io.suggest.css.Css
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ScalaComponent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 20:30
  * Description: React-утиль для форм.
  */


/** Контейнер для рендера видимых инпутов в ЛК. */
object Forms {

  val InputCont = ScalaComponent.builder[String]("InputCont")
    .stateless
    .renderPC { (_, props, children) =>
      <.div(
        ^.`class` := Css.flat(Css.Input.INPUT, props),
        children
      )
    }
    //.propsDefault( Css.Size.S ) // TODO Чо делать-то?
    .build

}
