package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.m.mhdr.{PrevNodeBtnClick, ShowNavClick, ShowSearchClick}
import io.suggest.sc.sjs.vm.grid.GRoot
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.hdr.btns.HNodePrev
import io.suggest.sc.sjs.vm.layout.FsLoader
import io.suggest.sc.sjs.vm.search.SRoot
import io.suggest.sjs.common.msg.WarnMsgs
import org.scalajs.dom.Event

/** Аддон для поддержки состояния "голая плитка" без открытых панелей, карточек и прочего. */
trait Plain extends OnGrid {

  /**
   * Состояние, когда на на экране уже отрендерена плитка карточек или её часть,
   * в заголовке доступны все основные кнопки.
   */
  protected trait OnPlainGridStateT extends OnGridStateT with PanelGridRebuilder with INodeSwitchState {

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
        // Размыть фоновую плитку, если узкий экран.
        _maybeBlurGrid(sd0)

        // Отребилдить плитку карточек, создав новое состояние выдачи.
        val grid2 = RebuildGridOnPanelOpen(sd0, screen, sroot).execute()

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
      become(_navLoadListState)
    }

    /** Состояние начала и ожидания загрузки списка узлов, если требуется. */
    protected def _navLoadListState: FsmState


    override def receiverPart: Receive = {
      val _receiverPart: Receive = {
        // Сигнал нажатия на кнопку открытия панели поиска.
        case ShowSearchClick(event) =>
          _showSearchClick(event)
        // Сигнал нажатия на кнопку отображения панели навигации.
        case ShowNavClick(event) =>
          _showNavClick(event)
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

  }

}
