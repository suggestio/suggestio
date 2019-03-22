package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.MuiPaper
import diode.react.ModelProxy
import io.suggest.id.login.m.reg.MRegFinishS
import io.suggest.id.login.v.stuff.CheckBoxR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.19 17:59
  * Description: Компонент страницы подтверждения регистрации нового юзера.
  * Юзер прошёл гос.услуги, и в первый раз вернулся в suggest.io.
  */
class RegFinishR(
                  checkBoxR         : CheckBoxR,
                ) {

  type Props_t = MRegFinishS
  type Props = ModelProxy[Props_t]

  /** Состояние: model-коннекшены. */
  case class State(
                  )

  class Backend( $: BackendScope[Props, State] ) {

    def render(s: State): VdomElement = {
      MuiPaper()(
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
      )
    }
    .renderBackend[Backend]
    .build

  def apply( propsProxy: Props ) = component( propsProxy )

}
