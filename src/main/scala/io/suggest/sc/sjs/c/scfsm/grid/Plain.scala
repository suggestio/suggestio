package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.ust.StateToUrlT
import io.suggest.sc.sjs.m.mhdr.{PrevNodeBtnClick, ShowNavClick, ShowSearchClick}
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.vm.hdr.btns.HNodePrev
import io.suggest.sc.sjs.vm.layout.FsLoader
import io.suggest.sjs.common.msg.WarnMsgs
import org.scalajs.dom.Event

/** Аддон для поддержки состояния "голая плитка" без открытых панелей, карточек и прочего. */
trait Plain extends OnGrid with StateToUrlT {

  /**
   * Состояние, когда на на экране уже отрендерена плитка карточек или её часть,
   * в заголовке доступны все основные кнопки.
   */
  protected trait OnPlainGridStateT extends OnGridStateT with INodeSwitchState {

    /** Реакция на запрос отображения поисковой панели. */
    protected def _showSearchClick(): Unit = {
      become(_searchPanelOpeningState)
    }

    protected def _searchPanelOpeningState: FsmState


    protected def _showNavClick(): Unit = {
      become(_navLoadListState)
    }

    /** Состояние начала и ожидания загрузки списка узлов, если требуется. */
    protected def _navLoadListState: FsmState


    override def receiverPart: Receive = {
      val _receiverPart: Receive = {
        // Сигнал нажатия на кнопку открытия панели поиска.
        case _: ShowSearchClick =>
          _showSearchClick()
        // Сигнал нажатия на кнопку отображения панели навигации.
        case _: ShowNavClick =>
          _showNavClick()
        // Клик по кнопке возврата на выдачу предыдущего узла.
        case PrevNodeBtnClick(event) =>
          _goToPrevNodeClick(event)
      }
      _receiverPart.orElse( super.receiverPart )
    }


    /** Реакция на клик по кнопке возврата на предыдущий узел.
      * Надо извлечь id узла из кнопки и выполнить переход в выдачу указанного узла. */
    protected def _goToPrevNodeClick(event: Event): Unit = {
      val fslOpt = FsLoader.find()
      for (fsl <- fslOpt) {
        fsl.show()
      }
      val btnOpt = HNodePrev.ofEventTarget( event.currentTarget )
        .orElse {
          HNodePrev.find()
        }
      btnOpt match {
        case Some(btn) =>
          val sd1 = _stateData.withNodeSwitch( btn.adnId )
          become(_onNodeSwitchState, sd1)
        case None =>
          for (fsl <- fslOpt) {
            fsl.hide()
          }
          warn(WarnMsgs.BACK_TO_UNDEFINED_NODE)
      }
    }

    /**
      * Укороченная реакция на popstate во время голой плитки.
      *
      * @param sdNext Распарсенные данные нового состояния из URL.
      */
    override def _handleStateSwitch(sdNext: MScSd): Unit = {
      if (sdNext.focused.isEmpty) {
        if (sdNext.nav.panelOpened) {
          _showNavClick()
        } else if (sdNext.search.opened) {
          _showSearchClick()
        }

      } else {
        super._handleStateSwitch(sdNext)
      }
    }

  }

}
