package io.suggest.lk.nodes.form

import io.suggest.lk.nodes.LkNodesConst
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.lk.nodes.form.m.MLkNodesRoot.MLknRootFastEq
import io.suggest.lk.nodes.form.m.MLknPopups.MLknPopupsFastEq
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import com.softwaremill.macwire._
import io.suggest.init.routed.InitRouter
import io.suggest.lk.r.popup.PopupsContR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:26
  * Description: Система инициализации формы управления узлами.
  */
trait LkNodesInitRouter extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.LkNodesForm) {
      initLkNodesForm()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /**
    * Логика инициализации проживает здесь.
    */
  private def initLkNodesForm(): Unit = {

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    val modules = wire[LkNodesModule]

    // Инициализировать circuit
    val circuit = modules.lkNodesFormCircuit

    // Рендер формы.
    val formR = circuit.wrap(identity(_))( modules.lkNodesFormR.component.apply )
    val formTarget = VUtil.getElementByIdOrNull[HTMLDivElement]( LkNodesConst.FORM_CONT_ID )
    formR.renderIntoDOM(formTarget )

    // Рендер компонента попапов.
    val popsContR = circuit.wrap(identity(_))( modules.lknPopupsR.component.apply )
    val popsContTarget = PopupsContR.initDocBody()
    popsContR.renderIntoDOM(popsContTarget)
  }

}

