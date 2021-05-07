package io.suggest.bill.cart

import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.view.VUtil
import japgolly.univeq._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 17:42
  * Description: Initializer for Cart react-form.
  */
trait CartPageInit extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.LkCartPageForm) {
      _doInit()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Start the Cart form. */
  private def _doInit(): Unit = {
    val modules = new CartModules
    val circuit = modules.cartPageCircuit

    // Render cart into the DOM:
    val cartFormCont = VUtil.getElementById[html.Div]( CartConstants.FORM_ID ).get
    val formContent = circuit.wrap( identity(_) )( modules.orderR.component.apply )
    formContent.renderIntoDOM( cartFormCont )
  }

}
