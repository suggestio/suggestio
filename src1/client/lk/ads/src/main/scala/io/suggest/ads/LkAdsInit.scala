package io.suggest.ads

import io.suggest.ads.m.AdsScroll
import io.suggest.sjs.common.vm.spa.LkPreLoader
import japgolly.univeq._
import japgolly.scalajs.react.vdom.Implicits._
import io.suggest.common.event.DomEvents
import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.scalajs.dom.UIEvent
import org.scalajs.dom.raw.{HTMLDivElement, HTMLDocument}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 19:15
  * Description: Инициализатор react-формы LkAds, т.е. управления картоками узла.
  */
trait LkAdsInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.LkAdsForm) {
      _doInit()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Запуск непосредственной инициализации узла. */
  private def _doInit(): Unit = {
    import io.suggest.ads.m.MLkAdsRoot.MLkAdsRootFastEq

    // Инициализировать хранилку ссылки на гифку прелоадера, т.к. тот будет стёрт входе react-рендера.
    LkPreLoader.PRELOADER_IMG_URL

    val modules = new LkAdsModule
    val circuit = modules.lkAdsCircuit

    // Линкуем с компонентом и html-страницей.
    circuit
      .wrap(identity(_))( mproxy => modules.lkAdsFormR(mproxy) )
      .renderIntoDOM( VUtil.getElementByIdOrNull[HTMLDivElement]( LkAdsFormConst.FORM_CONT_ID ) )

    // Подписаться на события скроллинга. scroll-контейнер пока висит на уровне html-тега.
    WindowVm()
      .addEventListener( DomEvents.SCROLL ) { e: UIEvent =>
        // Вроде есть какой-то нормальный метод определения scroll-контейнейра. Сейчас это documentElement.
        val scrollTop = e.target.asInstanceOf[HTMLDocument].documentElement.scrollTop
        val action = AdsScroll( scrollTop )
        circuit.dispatch( action )
      }

  }

}
