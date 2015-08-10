package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.HideNavClick
import io.suggest.sc.sjs.m.msrv.nodes.find.{MFindNodesResp, MFindNodesArgsEmpty, MFindNodesArgsDflt, MFindNodes}
import io.suggest.sc.sjs.vm.hdr.btns.nav.HShowNavBtn
import io.suggest.sc.sjs.vm.nav.NRoot
import io.suggest.sc.sjs.vm.nav.nodelist.NlContent
import io.suggest.sjs.common.util.ISjsLogger
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 15:54
 * Description: Аддон для состояний сетки с открытой панелью.
 */
trait OnGridNav extends ScFsmStub with ISjsLogger {

  protected trait _OnGridNav extends FsmState with PanelGridRebuilder {
    override def receiverPart: PartialFunction[Any, Unit] = {
      case HideNavClick(event) =>
        val sd0 = _stateData
        for (nroot <- NRoot.find(); screen <- sd0.screen) {
          // Визуально отобразить панель
          nroot.hide()
          // Скрыть кнопку показа панели.
          for (showBtn <- HShowNavBtn.find()) {
            showBtn.show()
          }
          val grid2 = _rebuildGridOnPanelChange(sd0, screen, nroot)

          val sd1 = sd0.copy(
            grid = grid2
          )

          become(_onHideNavState(sd1), sd1)
        }
    }

    /** Состояние с закрытой nav-панелью. */
    protected def _onHideNavState(sd1: SD): FsmState
  }


  /** Состояние отображения панели и загрузки списка узлов для навигации. */
  protected trait OnGridNavLoadList extends _OnGridNav {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for (nlContent <- NlContent.find() if nlContent.isEmpty) {
        // Запустить запрос к серверу на тему поиска узлов
        val searchNodesArgs = new MFindNodesArgsEmpty with MFindNodesArgsDflt {}
        val fut = MFindNodes.findNodes(searchNodesArgs)
        // Когда запрос выполниться, надо залить данные в DOM
        fut onComplete { case res =>
          val event = res match {
            case Success(resp)  => resp
            case failure        => failure
          }
          _sendEvent(event)
        }
      }

      for (nroot <- NRoot.find(); screen <- sd0.screen) {
        // Визуально отобразить панель
        nroot.show()
        // Скрыть кнопку показа панели.
        for (showBtn <- HShowNavBtn.find()) {
          showBtn.hide()
        }
        val grid2 = _rebuildGridOnPanelChange(sd0, screen, nroot)

        val sd1 = sd0.copy(
          grid = grid2
        )
        // TODO Обновить данные состояния выдачи.

        _stateData = sd1
      }
    }

    protected def _navPanelReadyState: FsmState

    // TODO Opt Оптимальнее будет super.receiverPart приклеивать справа, а не слева.
    override def receiverPart: PartialFunction[Any, Unit] = super.receiverPart orElse {
      // Положительный ответ от сервера со списком узлов.
      case resp: MFindNodesResp =>
        for (nlContent <- NlContent.find()) {
          nlContent.setContent(resp.nodeListHtml)
          // Для развернутого элемента исправить высоту.
          for (nlCont <- nlContent.container; exp1 <- nlCont.findFirstExpanded; screen <- _stateData.screen) {
            exp1.fixHeightExpanded(screen, nlCont.layersCount)
          }
        }

      // Ошибка ответа с сервера. Надо перещелкнуть на новое состояние
      case Failure(ex) =>
        // В логах отмечаемся и на следующее состояние переключаемся.
        error("E41", ex)
        become(_navPanelReadyState)
    }
  }

}
