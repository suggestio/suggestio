package io.suggest.maps.r

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 22:14
  * Description: Плашка с ошибкой по инициализации гео-карты.
  */
object MapInitFailR {

  type Props = ModelProxy[Pot[_]]

  val component = ReactComponentB[Props]("RcvrMarkersFail")
    .stateless
    .render_P { props =>
      for (ex <- props().exceptionOption) yield {
        <.div(
          ^.`class` := Css.flat( Css.Lk.Adv.Su.CONTAINER, Css.Colors.RED ),
          Messages( MsgCodes.`Error` ),
          ": ",
          Messages( MsgCodes.`Unable.to.initialize.map` ),
          <.br,
          ex.toString
        ): ReactElement
      }
    }
    .build

  def apply(potProxy: Props) = component(potProxy)

}
