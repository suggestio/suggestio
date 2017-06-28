package io.suggest.lk.nodes.form

import io.suggest.lk.nodes.LkNodesConst
import io.suggest.lk.nodes.form.r.LkNodesFormR
import io.suggest.lk.nodes.form.r.pop.LknPopupsR
import io.suggest.lk.pop.PopupsContR
import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.lk.nodes.form.m.MLkNodesRoot.MLknRootFastEq
import io.suggest.lk.nodes.form.m.MLknPopups.MLknPopupsFastEq
import org.scalajs.dom.raw.HTMLDivElement
import japgolly.scalajs.react.vdom.Implicits._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:26
  * Description: Система инициализации формы управления узлами.
  */
trait LkNodesInitRouter extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.LkNodesForm) {
      Future {
        initLkNodesForm()
      }
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

    // Инициализировать circuit
    val circuit = LkNodesFormCircuit

    // Рендер формы.
    val formR = circuit.wrap(m => m)(LkNodesFormR.apply)
    val formTarget = VUtil.getElementByIdOrNull[HTMLDivElement]( LkNodesConst.FORM_CONT_ID )
    formR.renderIntoDOM(formTarget )

    // Рендер компонента попапов.
    val popsContR = circuit.wrap(_.popups)( LknPopupsR.apply )
    val popsContTarget = PopupsContR.initDocBody()
    popsContR.renderIntoDOM(popsContTarget)
  }

}

