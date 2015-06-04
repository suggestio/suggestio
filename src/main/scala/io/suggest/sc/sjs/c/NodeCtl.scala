package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sc.sjs.m.mgrid.MGrid
import io.suggest.sc.sjs.m.msc.fsm.{MScState, MScFsm}
import io.suggest.sc.sjs.m.msrv.index.MNodeIndex
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sc.sjs.v.layout.LayoutView
import scala.scalajs.concurrent.JSExecutionContext
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
    MGrid.resetState()
    val inxFut = MNodeIndex.getIndex(adnIdOpt)

    // Начинаем инициализацию в отсутствие конкретных данных.
    GridCtl.resetAdsPerLoad()

    for {
      minx <- inxFut
    } yield {
      // Сохранить новое состояние выдачи.
      MScFsm.pushState(
        MScState(
          rcvrAdnId = minx.adnIdOpt
        )
      )

      // Сразу запускаем запрос к серверу за рекламными карточками.
      // Таким образом, под прикрытием welcome-карточки мы отфетчим и отрендерим плитку в фоне.
      val findAdsFut = GridCtl.askMoreAds()

      // TODO Выставить новый заголовок окна

      // Стереть старый layout, создать новый. Кешируем
      val l = LayoutView.redrawLayout()

      // Модифицировать текущее отображение под узел, отобразить welcome-карточку, если есть.
      LayoutView.showIndex(minx.html, layoutDiv = l.layoutDiv, rootDiv = l.rootDiv)

      // Инициализация welcomeAd.
      val wcHideFut = NodeWelcomeCtl.handleWelcome()

      GridCtl.initNewLayout(wcHideFut)
      // Когда grid-контейнер инициализирован, можно рендерить полученные карточки.
      findAdsFut onSuccess { case resp =>
        // Анимацию размещения блоков можно отключить, если welcome-карточка закрывает собой всё это.
        val noWelcome = wcHideFut.isCompleted
        GridCtl.newAdsReceived(resp, isAdd = false, withAnim = noWelcome)
      }

      NavPanelCtl.initNav()

      // TODO В оригинале была проверка isGeo, + сокрытие exit-кнопки и отображение nav-кнопки.
      // Этот фунционал был перенесен в шаблон, exit спилено там же.
      //NavPaneView.showNavShowBtn(isShown = true)

      if (isFirstRun) {
        DocumentView.initDocEvents()
        wcHideFut onComplete { case _ =>
          // Очищать фон нужно только первый раз. При последующей смене узла это не требуется уже.
          LayoutView.eraseBg(l.rootDiv)
        }
      }

      // В порядке очереди запустить инициализацию панели поиска.
      wcHideFut.onComplete { case _ =>
        SearchPanelCtl.initNodeLayout()
      }(JSExecutionContext.queue)

      LayoutView.setWndClass(l.layoutDiv)
    }
  }


}
