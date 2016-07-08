package io.suggest.sc.sjs.c.scfsm.nav

import io.suggest.sc.sjs.c.scfsm.grid.OnGrid
import io.suggest.sc.sjs.c.scfsm.ust.StateToUrlT
import io.suggest.sc.sjs.m.mhdr.{HideNavClick, LogoClick}
import io.suggest.sc.sjs.m.mnav.NodeListClick
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.m.msrv.nodes.find.{MFindNodes, MFindNodesArgsDfltImpl, MFindNodesResp}
import io.suggest.sc.sjs.vm.nav.nodelist.NlContent
import io.suggest.sc.sjs.vm.nav.nodelist.glay.{GlayCaption, GlayNode}
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.vm.Vm
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.{Event, KeyboardEvent, Node}

import scala.util.Failure

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 15:54
 */


/** Аддон для состояний сетки с открытой панелью. */
trait OnGridNav extends OnGrid with StateToUrlT {

  protected trait _OnGridNav extends OnGridStateT {

    private def _receiverPart: Receive = {
      case HideNavClick(event) =>
        _handleHideNav()
      case _: LogoClick =>
        _handleHideNav()
    }

    override def receiverPart: Receive = {
      _receiverPart.orElse( super.receiverPart )
    }

    override def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)
      if (event.keyCode == KeyCode.Escape)
        _handleHideNav()
    }

    protected def _handleHideNav(): Unit = {
      // Убрать размывку плитки, если она была, не проверяя размеры экрана на всякий случай.
      _unBlurGrid()

      val sd1 = NavUtil.hide( _stateData )
      become(_onHideNavState, sd1)

      State2Url.pushCurrState()
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
          override def currAdnId = sd0.common.adnIdOpt
        }
        val fut = MFindNodes.findNodes(searchNodesArgs)
        // Когда запрос выполниться, надо залить данные в DOM
        _sendFutResBack(fut)
      }

      // Размыть плитку в фоне, если экран маловат.
      _maybeBlurGrid(sd0)
      val sd2 = NavUtil.show(sd0)

      _stateData = sd2
      State2Url.pushCurrState()

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
        layerIndex  <- {
          exp1.fixHeightExpanded(sd0.common.screen, nlContainer.layersCount, sd0.common.browser)
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

    override def receiverPart: Receive = super.receiverPart.orElse {
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
          if (clickedAdnId == sd0.common.adnIdOpt) {
            // Клик по текущему узлу. Нужно просто скрыть панель навигации.
            _handleHideNav()
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
              } {
                body.show()
                body.fixHeightExpanded(sd0.common.screen, layCaption.container.layersCount, sd0.common.browser)
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
      for (cbody <- caption.body) {
        cbody.hide()
      }
    }

    protected def _glayShowHideState: FsmState = this

    /**
      * Укороченная реакция на popstate во время открытой панели навигации.
      *
      * @param sdNext Распарсенные данные нового состояния из URL.
      */
    override def _handleStateSwitch(sdNext: MScSd): Unit = {
      if (sdNext.focused.isEmpty && !sdNext.isAnySidePanelOpened) {
        _handleHideNav()
      } else {
        super._handleStateSwitch(sdNext)
      }
    }
  }

}
