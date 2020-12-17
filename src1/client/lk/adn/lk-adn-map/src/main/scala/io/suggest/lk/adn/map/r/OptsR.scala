package io.suggest.lk.adn.map.r

import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.17 10:27
  * Description: React-компонент выбора опций размещения узла на карте.
  *
  * 2017-06-02: Все две опции-галочки объеденины в одну обязательную галочку, которая не требует рендера.
  */
final class OptsR {

  type Props = ModelProxy[_]

  protected class Backend($: BackendScope[Props, Unit]) {

    def render(): VdomElement = {
      <.div(
        <.br,

        Messages( MsgCodes.`You.can.place.adn.node.on.map.below` ),
        <.br,
        <.br,

        Messages( MsgCodes.`Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
