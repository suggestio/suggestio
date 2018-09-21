package io.suggest.bill.cart

import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.view.VUtil
import japgolly.univeq._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 17:42
  * Description: Поддержка инициализации для react-формы корзины/биллинга.
  */
trait CartPageInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget): Unit = {
    if (itg ==* MInitTargets.LkCartPageForm) {
      _doInit()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Произвести запуск корзины. */
  private def _doInit(): Unit = {
    val modules = new CartModules

    // Выполнить инициализацию circuit'а:
    val circuit = modules.cartPageCircuit

    // Рендер корзины на экране:
    // Найти на странице контейнер для формы и отрендерить:
    val cartFormCont = VUtil.getElementById( CartConstants.FORM_ID ).get
    val formContent = circuit.wrap( identity(_) )( modules.cartR.apply )
    formContent.renderIntoDOM( cartFormCont )
  }

}
