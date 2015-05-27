package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.msc.MScState
import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sc.sjs.v.inx.ScIndex
import io.suggest.sc.sjs.v.layout.Layout
import io.suggest.sc.sjs.v.nav.NavPaneView
import io.suggest.sc.sjs.v.search.SearchPanelView
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 12:13
 * Description: Контроллер для узлов. Начинался с переключения узлов.
 */
object NodeCtl extends CtlT {

  /**
   * Полная процедура переключения на другой узел.
   *
   * Логика работы исходной версии системы:
   * 1. Отправить запрос index.
   * 2. Запустить анимацию выхода из текущего состояния выдачи.
   * 3. Получив index, отобразить welcome, запустить получение списка карточек.
   * 4. Получив список карточек, сразу отрендерить его.
   * 5. Дождавшись завершения welcome, отобразить список карточек.
   *
   * @param adnIdOpt id узла, если известен.
   *                 None значит пусть сервер сам решит, на какой узел переключаться.
   */
  def switchToNode(adnIdOpt: Option[String], isFirstRun: Boolean = false): Unit = {
    val inxFut = MNodeIndex.getIndex(adnIdOpt)
    implicit val _vctx = vctx
    GridCtl.resetAdsPerLoad()
    for {
      minx <- inxFut
    } yield {
      MScState.rcvrAdnId = minx.adnIdOpt

      // Сразу запускаем запрос к серверу за рекламными карточками.
      // Таким образом, под прикрытием welcome-карточки мы отфетчим и отрендерим плитку в фоне.
      GridCtl.needToLoadMoreAds()

      // Модифицировать текущее отображение под узел, отобразить welcome-карточку, если есть.
      Layout.reDrawLayout()(_vctx)
      ScIndex.showIndex(minx)(_vctx)

      // Инициализация welcomeAd.
      val wcHideFut = NodeWelcomeCtl.handleWelcome()

      GridCtl.initNewLayout(wcHideFut)

      NavPaneView.adjustNodeList()(_vctx)

      // TODO В оригинале была проверка isGeo, + сокрытие exit-кнопки и отображение nav-кнопки.
      // Этот фунционал был перенесен в шаблон, exit спилено там же.
      //NavPaneView.showNavShowBtn(isShown = true)(_vctx)

      if (isFirstRun) {
        DocumentView.initDocEvents()
        wcHideFut onComplete { case _ =>
          // Очищать фон нужно только первый раз. При последующей смене узла это не требуется уже.
          Layout.eraseBg()(_vctx)
        }
      }

      wcHideFut onComplete { case _ =>
        // Инициализация search-панели: поиск при наборе, например.
        SearchPanelView.initFtsField()
      }

      Layout.setWndClass()(_vctx)
    }
  }


}
