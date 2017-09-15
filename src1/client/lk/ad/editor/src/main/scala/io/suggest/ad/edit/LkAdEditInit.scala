package io.suggest.ad.edit

import com.softwaremill.macwire._
import io.suggest.ad.edit.m.MAeRoot.MAeRootFastEq
import io.suggest.ad.form.AdFormConstants
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.scalajs.react.vdom.Implicits._
import org.scalajs.dom.raw.HTMLDivElement

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:54
  * Description: Инициализатор react-формы редактирования карточки.
  */
trait LkAdEditInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget) = {
    if (itg == MInitTargets.LkAdEditR) {
      Future {
        initAdEditForm()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Выполнить инициализацию react-редактора карточки. */
  private def initAdEditForm(): Unit = {
    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    val modules = wire[LkAdEditModule]

    // Инициализировать circuit
    val circuit = modules.lkAdEditCircuit

    // Инициализировать quill для редактора карточе
    modules.quillSioModule.quillInit.forAdEditor()

    // Произвести рендер компонента формы:
    val formComponent = circuit.wrap(m => m)(modules.lkAdEditFormR.apply)
    val formTarget = VUtil.getElementByIdOrNull[HTMLDivElement]( AdFormConstants.AD_EDIT_FORM_CONT_ID )
    formComponent.renderIntoDOM( formTarget )
  }

}
