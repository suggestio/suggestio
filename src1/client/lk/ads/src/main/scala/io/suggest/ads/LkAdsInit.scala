package io.suggest.ads

import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.univeq._
import japgolly.scalajs.react.vdom.Implicits._
import io.suggest.ads.m.MLkAdsRoot.MLkAdsRootFastEq
import io.suggest.sjs.common.view.VUtil
import org.scalajs.dom.raw.HTMLDivElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 19:15
  * Description: Инициализатор react-формы LkAds, т.е. управления картоками узла.
  */
trait LkAdsInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget): Unit = {
    if (itg ==* MInitTargets.LkAdsForm) {
      _doInit()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Запуск непосредственной инициализации узла. */
  private def _doInit(): Unit = {
    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    val modules = new LkAdsModule
    val circuit = modules.lkAdsCircuit

    // Линкуем с компонентом и html-страницей.
    circuit
      .wrap(identity(_))( mproxy => modules.lkAdsFormR(mproxy) )
      .renderIntoDOM( VUtil.getElementByIdOrNull[HTMLDivElement]( LkAdsFormConst.FORM_CONT_ID ) )

    // TODO Рендер контейнера попапов - по аналогии с LkAdvGeoInit
  }

}
