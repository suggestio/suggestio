package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.{ShowNavClick, ShowSearchClick}
import org.scalajs.dom.Event

/** Аддон для поддержки состояния "голая плитка" без открытых панелей, карточек и прочего. */
trait OnPlainGrid extends OnGrid {

   /**
    * Состояние, когда на на экране уже отрендерена плитка карточек или её часть,
    * в заголовке доступны все основные кнопки.
    */
   protected trait OnPlainGridStateT extends OnGridStateT {

     /** Реакция на запрос отображения поисковой панели. */
     protected def _showSearchClick(event: Event): Unit = {
       // TODO Показать панель, выставить соотв.флаг в состояние выдачи, перестроить карточки.
       ???
     }

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
