package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.{ShowNavClick, ShowSearchClick}
import io.suggest.sc.sjs.m.msrv.nodes.find.{MFindNodesArgsEmpty, MFindNodes, MFindNodesArgsDflt}
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.hdr.btns.nav.HShowNavBtn
import io.suggest.sc.sjs.vm.nav.NRoot
import io.suggest.sc.sjs.vm.nav.nodelist.NlContent
import io.suggest.sc.sjs.vm.search.SRoot
import org.scalajs.dom.Event
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/** Аддон для поддержки состояния "голая плитка" без открытых панелей, карточек и прочего. */
trait OnPlainGrid extends OnGrid {

   /**
    * Состояние, когда на на экране уже отрендерена плитка карточек или её часть,
    * в заголовке доступны все основные кнопки.
    */
   protected trait OnPlainGridStateT extends OnGridStateT with PanelGridRebuilder {

     /** Реакция на запрос отображения поисковой панели. */
     protected def _showSearchClick(event: Event): Unit = {
       val sd0 = _stateData
       for (sroot <- SRoot.find(); screen <- sd0.screen) {
         // Показать панель
         sroot.show()
         // Сменить набор кнопок в заголовке.
         for (header <- HRoot.find()) {
           header.showBackToIndexBtns()
         }

         // Отребилдить плитку карточек, создав новое состояние выдачи.
         val grid2 = _rebuildGridOnPanelChange(sd0, screen, sroot)

         val sd1 = sd0.copy(
           search = sd0.search.copy(
             opened = true
           ),
           grid = grid2
         )

         // Сменить состояние на то, где открыта панель поиска.
         become(_nextStateSearchPanelOpened(sd1), sd1)
       }
     }

     protected def _nextStateSearchPanelOpened(sd1: SD): FsmState


     protected def _showNavClick(event: Event): Unit = {
       // TODO Запустить получение списка узлов с сервера, если список ещё не получен.
       for (nlContent <- NlContent.find() if nlContent.isEmpty) {
         // Запустить запрос к серверу на тему поиска узлов
         val searchNodesArgs = new MFindNodesArgsEmpty with MFindNodesArgsDflt {}
         val fut = MFindNodes.findNodes(searchNodesArgs)
         fut onSuccess { case resp =>
           nlContent.setContent( resp.nodeListHtml )
           // TODO Для развернутого элемента исправить высоту.
           for (nlCont <- nlContent.container; exp1 <- nlCont.findFirstExpanded) {
             ???
           }
         }
       }

       val sd0 = _stateData
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

         become(_nextStateNavPanelOpened(sd1), sd1)
       }
     }

     protected def _nextStateNavPanelOpened(sd1: SD): FsmState


     override def receiverPart: PartialFunction[Any, Unit] = super.receiverPart orElse {
       // Сигнал нажатия на кнопку открытия панели поиска.
       case ShowSearchClick(event) =>
         _showSearchClick(event)

       // Сигнал нажатия на кнопку отображения панели навигации.
       case ShowNavClick(event) =>
         _showNavClick(event)
     }
   }

}
