package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.c.scfsm.grid.{OnGrid, PanelGridRebuilder}
import io.suggest.sc.sjs.m.mhdr.{HideNavClick, LogoClick}
import io.suggest.sc.sjs.m.mnav.NodeListClick
import io.suggest.sc.sjs.m.msrv.nodes.find.{MFindNodes, MFindNodesArgsDfltImpl, MFindNodesResp}
import io.suggest.sc.sjs.vm.hdr.btns.HBtns
import io.suggest.sc.sjs.vm.hdr.btns.nav.HShowNavBtn
import io.suggest.sc.sjs.vm.nav.NRoot
import io.suggest.sc.sjs.vm.nav.nodelist.glay.{GlayCaption, GlayNode}
import io.suggest.sc.sjs.vm.nav.nodelist.NlContent
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.vm.Vm
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.{Event, KeyboardEvent, Node}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.Failure

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 15:54
 * Description: Аддон для состояний сетки с открытой панелью.
 */
trait OnGridNav extends OnGrid with UrlStateT {

  protected trait _OnGridNav extends OnGridStateT with PanelGridRebuilder {

    private def _receiverPart: Receive = {
      case HideNavClick(event) =>
        _hideNav()
      case _: LogoClick =>
        _hideNav()
    }

    override def receiverPart: Receive = {
      _receiverPart orElse super.receiverPart
    }

    override def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)
      if (event.keyCode == KeyCode.Escape)
        _hideNav()
    }

    protected def _hideNav(): Unit = {
      val sd0 = _stateData
      for (nroot <- NRoot.find(); screen <- sd0.screen) {
        // Визуально отобразить панель
        nroot.hide()

        // Скрыть кнопку показа панели.
        for (showBtn <- HShowNavBtn.find()) {
          showBtn.show()
        }

        // Убрать размывку плитки, если она была, не проверяя размеры экрана на всякий случай.
        _unBlurGrid()

        val grid2 = RebuildGridOnPanelClose(sd0, screen, nroot).execute()

        for (hbtns <- HBtns.find()) {
          hbtns.show()
        }

        val sd1 = sd0.copy(
          grid = grid2,
          nav = sd0.nav.copy(
            panelOpened = false
          )
        )
        become(_onHideNavState, sd1)

        UrlStates.pushCurrState()
      }
    }

    /** Состояние с закрытой nav-панелью. */
    protected def _onHideNavState: FsmState
  }


  /** Трейт состояние отображения панели и загрузки списка узлов для навигации. */
  protected trait OnGridNavLoadListStateT extends _OnGridNav {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      val nlContentOpt = NlContent.find()
      for (nlContent <- nlContentOpt if nlContent.isEmpty) {
        // Запустить запрос к серверу на тему поиска узлов
        val searchNodesArgs = new MFindNodesArgsDfltImpl {
          override def currAdnId = sd0.adnIdOpt
        }
        val fut = MFindNodes.findNodes(searchNodesArgs)
        // Когда запрос выполниться, надо залить данные в DOM
        _sendFutResBack(fut)
      }

      for (nroot <- NRoot.find(); screen <- sd0.screen) {
        // Визуально отобразить панель
        nroot.show()
        // Скрыть кнопку показа панели.
        for (showBtn <- HShowNavBtn.find()) {
          showBtn.hide()
        }
        val grid2 = RebuildGridOnPanelOpen(sd0, screen, nroot).execute()

        // Размыть плитку в фоне, если экран маловат.
        _maybeBlurGrid(sd0)

        val sd2 = sd0.copy(
          grid = grid2,
          nav = sd0.nav.copy(
            panelOpened = true
          )
        )
        _stateData = sd2

        UrlStates.pushCurrState()
      }

      for (hbtns <- HBtns.find()) {
        hbtns.hide()
      }

      // Список узлов на панели уже загружен, нужно сразу перещелкнуть на следующее состояние.
      if (nlContentOpt.exists(_.nonEmpty)) {
        become(_navPanelReadyState)
      }

    }

    protected def _navPanelReadyState: FsmState

    /** Реакция на положительный ответ от сервера со списком узлов. */
    protected def _findNodesResp(resp: MFindNodesResp): Unit = {
      val sd0 = _stateData
      // Какой-то for-велосипед тут получился в ходе рефакторинга говнокода.
      val sd1Opt = for {
        nlContent   <- NlContent.find()
        nlContainer <- {
          nlContent.setContent(resp.nodeListHtml)
          nlContent.container
        }
        exp1        <- {
          nlContainer.initLayout()
          nlContainer.findFirstExpanded
        }
        screen      <- _stateData.screen
        layerIndex  <- {
          exp1.fixHeightExpanded(screen, nlContainer.layersCount, sd0.common.browser)
          exp1.layerIndexOpt
        }
      } yield {
          sd0.copy(
            nav = sd0.nav.copy(
              currGlayIndex = Some(layerIndex)    // TODO Opt тут пересоздается ранее раскрытый Option.
            )
          )
        }
      become(_navPanelReadyState, sd1Opt getOrElse sd0)
    }

    /** Реакция на проблемы при получении списка узлов.
      * Надо перещелкнуть на новое состояние */
    protected def _findNodesFailed(ex: Throwable): Unit = {
      // В логах отмечаемся и на следующее состояние переключаемся.
      error(ErrorMsgs.FIND_ADS_REQ_FAILED, ex)
      become(_navPanelReadyState)
    }

    override def receiverPart: Receive = super.receiverPart orElse {
      // Положительный ответ от сервера со списком узлов.
      case resp: MFindNodesResp =>
        _findNodesResp(resp)
      // Ошибка ответа с сервера.
      case Failure(ex) =>
        _findNodesFailed(ex)
    }

  }


  /** Трейт состояния готовности к работе панели вместе со списком карточек. */
  protected trait OnGridNavReadyStateT extends _OnGridNav with INodeSwitchState {

    private def _receiverPart: Receive = {
      case NodeListClick(event) =>
        _navNodeListClick(event)
    }

    override def receiverPart: Receive = {
      _receiverPart
        .orElse(super.receiverPart)
    }

    protected def _navNodeListClick(event: Event): Unit = {
      val clickedNode = event.target.asInstanceOf[Node] // Node максимум, т.к. клик может быть по узлам svg
      if (clickedNode != null) {
        val sd0 = _stateData
        // Тестируем на принадлежность к заголовку слоя
        val safeClickedNode = Vm( clickedNode )
        val adnNodeOpt = GlayNode.findUpToNode(safeClickedNode)

        if (adnNodeOpt.isDefined) {
          // Юзер кликнул по узлу в списке узлов.
          val glayNode = adnNodeOpt.get
          val clickedAdnId = glayNode.adnIdOpt
          if (clickedAdnId == sd0.adnIdOpt) {
            // Клик по текущему узлу. Нужно просто скрыть панель навигации.
            _hideNav()
          } else {
            // Сменить текущий узел на выбранный пользователем.
            val sd1 = sd0.withNodeSwitch( glayNode.adnIdOpt )
            become(_onNodeSwitchState, sd1)
          }

        } else {

          // Юзер кликнул по заголовку геослоя узлов.
          for (layCaption <- GlayCaption.findUpToNode(safeClickedNode)) {
            val indexClickedOpt = layCaption.layerIndexOpt

            val currIndexOpt2: Option[Int] = if (sd0.nav.currGlayIndex == indexClickedOpt) {
              // Клик по раскрытом слою -- скрытие текущего геослоя
              _layHide(layCaption)
              None

            } else {
              // Клик по скрытому слою. Скрыть текущий слой, если есть.
              for {
                glayIndexPrev <- sd0.nav.currGlayIndex
                caption       <- GlayCaption.find(glayIndexPrev)
              } {
                _layHide(caption)
              }
              // Отобразить кликнутый слой.
              layCaption.activate()
              for {
                body    <- layCaption.body
                screen  <- sd0.screen
              } {
                body.show()
                body.fixHeightExpanded(screen, layCaption.container.layersCount, sd0.common.browser)
              }
              indexClickedOpt
            }
            val sd1 = sd0.copy(
              nav = sd0.nav.copy(
                currGlayIndex = currIndexOpt2
              )
            )
            become(_glayShowHideState, sd1)
          }

        } // else
      }
    }

    private def _layHide(caption: GlayCaption): Unit = {
      caption.deactivate()
      caption.body.foreach( _.hide() )
    }

    protected def _glayShowHideState: FsmState = this
  }
}
