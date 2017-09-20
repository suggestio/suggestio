package io.suggest.maps.r

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ScalaComponent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 22:14
  * Description: Плашка с ошибкой по инициализации гео-карты.
  */
object MapInitFailR {

  type Props = ModelProxy[Pot[_]]

  val component = ScalaComponent.builder[Props]("RcvrMarkersFail")
    .stateless
    .render_P { props =>
      props().exceptionOption.whenDefinedEl { ex =>
        <.div(
          ^.`class` := Css.flat( Css.Lk.Adv.Su.CONTAINER, Css.Colors.RED ),
          Messages( MsgCodes.`Error` ),
          ": ",
          Messages( MsgCodes.`Unable.to.initialize.map` ),
          <.br,
          ex.toString
        )
      }
    }
    .build

  def apply(potProxy: Props) = component(potProxy)

}
