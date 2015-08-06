package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.{ShowNavClick, ShowSearchClick}
import io.suggest.sc.sjs.vm.grid.GContent
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.search.SRoot
import org.scalajs.dom.Event

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
         val sd1 = _rebuildGridOnPanelChange(sd0, screen, sroot)

         // Сменить состояние на то, где открыта панель поиска.
         become(_nextStateSearchPanelOpened(sd1), sd1)
       }
     }

     protected def _nextStateSearchPanelOpened(sd1: SD): FsmState

     override def receiverPart: PartialFunction[Any, Unit] = super.receiverPart orElse {
       // Сигнал нажатия на кнопку открытия панели поиска.
       case ShowSearchClick(event) =>
         _showSearchClick(event)

       // Сигнал нажатия на кнопку отображения панели навигации.
       case ShowNavClick(event) =>
         // TODO Показать панель навигации, обновить данные состояния выдачи, перестроить карточки.
         ???
     }
   }


 }
