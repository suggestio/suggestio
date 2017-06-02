package io.suggest.lk.adn.map.r

import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.17 10:27
  * Description: React-компонент выбора опций размещения узла на карте.
  *
  * 2017-06-02: Все две опции-галочки объеденины в одну обязательную галочку, которая не требует рендера.
  */
object OptsR {

  type Props = ModelProxy[_]

  protected class Backend($: BackendScope[Props, Unit]) {

    def render(): ReactElement = {
      <.div(
        <.br,

        Messages( MsgCodes.`You.can.place.adn.node.on.map.below` ),
        <.br,
        <.br,

        Messages( MsgCodes.`Your.sc.will.be.opened.auto.when.user.geolocated.inside.circle` )
      )
    }

  }


  val component = ReactComponentB[Props]("Opts")
    .stateless
    .renderBackend[Backend]
    .build


  def apply(optsProxy: Props) = component(optsProxy)

}
