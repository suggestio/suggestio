package io.suggest.react.r

import diode.react.ModelProxy
import io.suggest.spa.DAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.05.2020 15:21
  * Description: Компонент-перехватчик ошибок из дочерних компонентов.
  */
object CatchR {

  type Props = ModelProxy[String]

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_C { propsChildren =>
      <.div(
        propsChildren
      )
    }
    .componentDidCatch { $ =>
      $.props.dispatchCB( ComponentCatch($.props.value, $.error) )
    }
    .build

}


/** Ошибка перехвачена каким-то компонентом. */
case class ComponentCatch( sourceId: String, error: ReactCaughtError ) extends DAction
