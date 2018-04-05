package io.suggest.adn.edit

import io.suggest.sjs.common.controller.InitRouter
import japgolly.univeq._
import com.softwaremill.macwire._
import io.suggest.sjs.common.view.VUtil
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:31
  * Description: Инициализатор формы редактирования Adn-узла.
  */
trait LkAdnEditInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget): Unit = {
    if (itg ==* MInitTargets.LkAdnEditForm) {
      main()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Запуск инициализации формы редактирования узла. */
  private def main(): Unit = {
    val module = wire[LkAdnEditModule]
    val circuit = module.lkAdnEditCircuit

    // Рендерим основную форму:
    circuit
      .wrap(identity(_))( module.lkAdnEditFormR.apply )
      .renderIntoDOM(
        VUtil.getElementByIdOrNull[HTMLDivElement]( NodeEditConstants.FORM_CONTAINER_ID )
      )

    // TODO Рендерить компонент сохранения в правый div.
  }

}

