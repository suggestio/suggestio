package io.suggest.n2.edge.edit

import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.view.VUtil
import japgolly.univeq._
import org.scalajs.dom
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 15:28
  * Description: Поддержка init-router.
  */
trait EdgeEditInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (MJsInitTargets.EdgeEditForm ==* itg) {
      _initForm()
    } else {
      super.routeInitTarget(itg)
    }
  }

  /** Запуск инициализации формы. */
  private def _initForm(): Unit = {
    val module = new EdgeEditModule

    // Найти контейнер для circuit.
    val container = VUtil
      .getElementById[dom.html.Div]( EdgeEditConst.FORM_ID )
      .get

    // Отрендерить форму в контейнер:
    module.edgeEditCircuit
      .wrap( identity(_) )( module.edgeEditFormR.component.apply )
      .renderIntoDOM( container )
  }

}
