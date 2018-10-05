package io.suggest.sys.mdr.v

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.sys.mdr.m.MMdrRefuseDialogS
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 22:36
  * Description: Компонент диалога отказа от размещения.
  */
class MdrDiaRefuseR {

  case class PropsVal(
                       state: MMdrRefuseDialogS,
                     )
  implicit object MdrDiaRefuseRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.state ===* b.state
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props): VdomElement = {
      <.div(
        // TODO MuiDialog
        "???"
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
