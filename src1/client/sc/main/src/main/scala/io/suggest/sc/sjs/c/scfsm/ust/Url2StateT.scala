package io.suggest.sc.sjs.c.scfsm.ust

import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.sc.router.SrvRouter
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.c.scfsm.foc.FocCommon
import io.suggest.sc.sjs.c.scfsm.nav.NavUtil
import io.suggest.sc.sjs.c.scfsm.search.SearchUtil
import io.suggest.sc.sjs.m.mfoc.{MFocCurrSd, MFocSd}
import io.suggest.sc.sjs.m.msc.{MGen, MScSd, PopStateSignal}
import io.suggest.sc.sjs.m.msearch.{MTabSwitchSignal, MTabs}
import io.suggest.sc.sjs.m.mtags.MTagInfo
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.text.UrlUtil2
import org.scalajs.dom

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.05.16 13:41
  * Description: Аддоны для утили десериализации состояний ScFsm.
  * Используется манипуляция кодом готовых состояний для достижения целей восстановления состояния.
  */

/** Простой интерфейс публичного API десереализатора. */
trait IUrl2State {

  protected def _urlHash = dom.window.location.hash
  def _parseFromUrlHash(urlHash: String = _urlHash): Option[MScSd]

  def _runInitState(sd0Opt: Option[MScSd]): Future[_]

  def _nodeReInitState(sdNext: MScSd): Unit

  /** Поддержка переключения на другое состояние по сигналу из истории браузера. */
  def _handlePopState(pss: PopStateSignal): Unit

}


/** Реализация аддона [[IUrl2State]].
  * Подмешивается прямо в [[io.suggest.sc.sjs.c.scfsm.ScFsm]].
  */
trait Url2StateT extends IUrl2State { scFsm: ScFsm.type =>

  import io.suggest.sc.ScConstants.ScJsState._


  /** Запуск инициализации системы из данных URL. */
  override def _parseFromUrlHash(urlHash: String): Option[SD] = {
    // Распарсить в карту токенов.
    val tokens: Map[String, String] = {
      UrlUtil2.clearUrlHash( urlHash )
        .map( MScSd.parseFromQs )
        .filter( _.nonEmpty )
        .getOrElse(Map.empty)
    }

    if (tokens.isEmpty) {
      // Нет qs, можно не продолжать.
      None

    } else {
      // Есть qs-токены, накатить их на состояние FSM.
      val sd1 = _applyQsTokens(tokens, _stateData)
      Some(sd1)
    }
  }   // _initFromCurrUrl()


  /** Инициализация системы на основе уже распарсенных токенов. */
  protected def _applyQsTokens(tokens: Map[String, String], sd0: SD): SD = {
    // Закидываем значение generation в состояние.
    val sd1Common = {
      val sd0Common = sd0.common
      tokens.get(GENERATION_FN)
        .flatMap(MGen.parse)
        .filter(_ != sd0Common.generation)
        .fold(sd0Common) { gen =>
          sd0Common.copy(
            generation = gen
          )
        }
    }

    // Десериализация geo-точки выдачи.
    val sd2Common = tokens.get(LOC_ENV_FN)
      .flatMap( MGeoPoint.fromString )
      .fold(sd1Common) { geoPos =>
        val geoLoc = MGeoLoc(
          point = geoPos
        )
        sd1Common.withGeoLoc( Some(geoLoc) )
      }

    // Десериализация гео-тега выдачи
    val tagInfoOpt = for {
      tagNodeId <- tokens.get(TAG_NODE_ID_FN)
      tagFace   <- tokens.get(TAG_FACE_FN)
    } yield {
      MTagInfo(
        nodeId = tagNodeId,
        face   = tagFace
      )
    }
    val sd3Common = tagInfoOpt.fold(sd2Common) { tagInfo =>
      sd2Common.withTagInfo( Some(tagInfo) )
    }

    // Загружаем данные состояния панели навигации.
    val sd1Nav = {
      val sd0Nav = sd0.nav
      val flag = tokens
        .get(GEO_SCR_OPENED_FN)
        .flatMap { geoPanelOpenedStr =>
          try {
            Some(geoPanelOpenedStr.toBoolean)
          } catch {
            case ex: Throwable =>
              LOG.warn(WarnMsgs.NAV_PANEL_OPENED_PARSE_ERROR, ex)
              None
          }
        }
        .getOrElseFalse
      sd0Nav.copy(
        panelOpened = flag
      )
    }

    // Загружаем данные состояния панели поиска.
    val sd1Search = {
      val sd0Search = sd0.search
      tokens.get(CAT_SCR_OPENED_FN)
        .flatMap { panelOpenedStr =>
          try {
            Some(panelOpenedStr.toBoolean)
          } catch {
            case ex: Throwable =>
              LOG.warn(WarnMsgs.SEARCH_PANEL_OPENED_PARSE_ERROR, ex)
              None
          }
        }
        .fold {
          sd0Search.copy(
            opened = false
          )
        } { flag =>
          // Определить текущую открытую вкладку поиска.
          val mtab = tokens.get(SEARCH_TAB_FN)
            .flatMap(MTabs.maybeWithName)
            .getOrElse(sd0Search.fsm.currTab)
          sd0Search.fsm ! MTabSwitchSignal(mtab)
          sd0Search.copy(
            opened  = flag
          )
        }
    }

    // Отрабатываем focused ad: просто залить id в состояние.
    val mFocSdOpt = for {
      focAdId <- tokens.get(FADS_CURRENT_AD_ID_FN)
      if focAdId.nonEmpty
    } yield {
      MFocSd(
        current = MFocCurrSd(
          madId           = focAdId,
          index           = 0,
          forceFocus      = true
        ),
        producerId = tokens.get(PRODUCER_ADN_ID_FN)
      )
    }

    // Отрабатываем id узла из URL, если есть.
    val nodeIdOpt = tokens.get(ADN_ID_FN)
    // Залить новый id узла в common-состояние.
    val sd4Common = if (nodeIdOpt != sd1Common.adnIdOpt) {
      sd3Common.withAdnId( nodeIdOpt )
    } else {
      sd3Common
    }

    // Собрать итоговые данные состояния.
    sd0.copy(
      common = sd4Common,
      nav    = sd1Nav,
      search = sd1Search,
      focused = mFocSdOpt
    )
  }


  /** Используя распарсенные данные состояния FSM, выбрать initial state этого FSM и применить его. */
  override def _runInitState(sd0Opt: Option[SD]): Future[_] = {
    val nextStateFut = sd0Opt.fold [Future[FsmState]] {
      Future.successful( _geoInitState )
    } { sd0 =>
      _stateData = sd0
      // Выполнить переход на новое состояние FSM.
      val c = sd0.common
      if (c.adnIdOpt.nonEmpty || c.geoLocOpt.nonEmpty) {
        // Задан id узла выдачи, перейти в него.
        val routerFut = SrvRouter.ensureJsRouter()
        for (_ <- routerFut) yield {
          new NodeIndex_Get_Wait_State
        }
      } else {
        // Есть токены, но конкретные ориентиры для выдачи не заданы.
        Future.successful( _geoInitState )
      }
    }
    for (nextState <- nextStateFut) yield {
      become(nextState)
      null
    }
  }


  /** Реакция на повторную инициализацию текущей выдачи в рамках существующего состояния. */
  override def _nodeReInitState(sdNext: MScSd): Unit = {
    val sd0 = _stateData

    // Нужно без welcome организовать всё.
    val nextState = if (sdNext.focused.nonEmpty) {
      // Запускать фокусировку на указанной карточке.
      _stateData = sd0.copy(
        focused = sdNext.focused
      )
      FocCommon.clearFocused()
      new FocStartingForAd

    } else {

      // Удалить focused-выдачу с экрана, если она там есть.
      if (sd0.focused.nonEmpty) {
        FocCommon.closeFocused()
        _stateData = sd0.notFocused
      }

      // Выявить нужное целевое состояние плитки узла.
      if (sdNext.nav.panelOpened) {
        new OnGridNavLoadListState
      } else if (sdNext.search.opened) {
        _stateData.search.fsm ! MTabSwitchSignal(sdNext.search.fsm.currTab)
        new OnGridSearchState
      } else {
        new OnPlainGridState
      }
    }
    become( nextState )
  }

  /** Реакция на сигнал popstate, т.е. когда юзер гуляет по истории браузера. */
  override def _handlePopState(pss: PopStateSignal): Unit = {
    // 2016.jul.12 Сверять URL с текущим значением window.location, отличается ли?
    // iOS 8.x web-app внезапно шлёт паразитные сигналы, дублирующие текущее состояние.

    val sdNextQsStr = MScSd.toQsStr( _stateData )
    if ( State2Url.currUrlQsEqualsTo(sdNextQsStr) ) {
      LOG.warn( WarnMsgs.POP_STATE_TO_SAME_STATE, msg = sdNextQsStr )

    } else {

      // Начинаем с отработки разных общих случаев изменения состояния.
      _parseFromUrlHash().fold [Unit] {
        // Отсутствие нового состояния, имитировать перезагрузку выдачи.
        become( _geoInitState )

      } { sdNext =>

        val sd0 = _stateData

        // - Изменение важных параметров выдачи, которая должна приводить к смене выдачи под корень.
        if ( sdNext.isScDiffers(sd0) ) {
          // Изменился id текущего узла выдачи. Выдача уже не будет прежней. Имитируем полное переключение узла.
          become( new NodeIndex_Get_Wait_State, sdNext )

        } else {
          // Корень выдачи тот же. Надо подогнать текущую выдачу под новые параметры состояния. Никаких welcome на экране отображать не надо.
          // Отрабатываем возможное изменение конфигурации боковых панелей в рамках узла/локации.

          // Если изменилось значение состояния распахнутости боковой панели навигации (слева), то внести изменения в DOM и исходное состояние.
          if (sd0.nav.panelOpened != sdNext.nav.panelOpened) {
            _stateData = NavUtil.invert(sd0)
          }

          // Отработать панель поиска: скрыть или показать, если состояние изменилось.
          val sd1 = _stateData
          if (sd1.search.opened != sdNext.search.opened) {
            _stateData = SearchUtil.invert(sd1)
          }

          // Наконец отработать конкретные изменения данных состояния на уровне текущего состояния.
          _state._handleStateSwitch(sdNext)
        }
      }

    }
  }

}
