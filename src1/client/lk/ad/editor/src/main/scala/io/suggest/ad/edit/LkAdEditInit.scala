package io.suggest.ad.edit

import com.softwaremill.macwire._
import io.suggest.ad.form.AdFormConstants
import io.suggest.init.routed.InitRouter
import io.suggest.lk.r.popup.PopupsContR
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.up.UploadConstants
import io.suggest.ww.WwMgr
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.univeq._
import org.scalajs.dom.raw.HTMLDivElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:54
  * Description: Инициализатор react-формы редактирования карточки.
  */
trait LkAdEditInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.LkAdEditR) {
      initAdEditForm()
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

    // Инициализировать quill для редактора карточек
    modules.quillSioModule.quillInit.forAdEditor()

    val rootRO = circuit.rootRO

    // Произвести рендер компонента формы:
    val formComponent = circuit.wrap(rootRO)( modules.lkAdEditFormR.component.apply )
    val formTarget = VUtil.getElementByIdOrNull[HTMLDivElement]( AdFormConstants.AD_EDIT_FORM_CONT_ID )
    formComponent.renderIntoDOM( formTarget )

    // Рендерить контейнер попапов...
    circuit
      .wrap(rootRO)( modules.laePopupsR.component.apply )
      .renderIntoDOM( PopupsContR.initDocBody() )

    // Требуются фоновые веб-воркеры для параллельного хэширования разными алгоритмами.
    WwMgr.start( UploadConstants.CleverUp.UPLOAD_FILE_HASHES.size )
  }

}
