package io.suggest.sc.sjs.c.scfsm.ust

import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.mfoc.{MFocCurrSd, MFocSd}
import io.suggest.sc.sjs.m.msc.{MGen, MScSd, MUrlUtil}
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sjs.common.msg.WarnMsgs
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

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
      MUrlUtil.clearUrlHash( urlHash )
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

    // Загружаем данные состояния панели навигации.
    val sd1Nav = {
      val sd0Nav = sd0.nav
      tokens.get(GEO_SCR_OPENED_FN)
        .flatMap { geoPanelOpenedStr =>
          try {
            Some(geoPanelOpenedStr.toBoolean)
          } catch {
            case ex: Throwable =>
              warn(WarnMsgs.NAV_PANEL_OPENED_PARSE_ERROR, ex)
              None
          }
        }
        .fold(sd0Nav) { flag =>
          sd0Nav.copy(
            panelOpened = flag
          )
        }
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
              warn(WarnMsgs.SEARCH_PANEL_OPENED_PARSE_ERROR, ex)
              None
          }
        }
        .fold(sd0Search) { flag =>
          // Определить текущую открытую вкладку поиска.
          val mtab = tokens.get(SEARCH_TAB_FN)
            .flatMap(MTabs.maybeWithName)
            .getOrElse(sd0Search.currTab)
          sd0Search.copy(
            opened  = flag,
            currTab = mtab
          )
        }
    }

    // Отрабатываем focused ad: просто залить id в состояние.
    val focAdIdRawOpt = tokens.get(FADS_CURRENT_AD_ID_FN)
    val mFocSdOpt = for {
      focAdId <- focAdIdRawOpt
      if focAdId.nonEmpty
    } yield {
      MFocSd(
        current = MFocCurrSd(
          madId           = focAdId,
          index           = 0,
          forceFocus      = true
        )
      )
    }

    // Отрабатываем id узла из URL, если есть.
    val nodeIdOpt = tokens.get(ADN_ID_FN)

    // Залить новый id узла в common-состояние.
    val sd2Common = if (nodeIdOpt != sd1Common.adnIdOpt) {
      sd1Common.copy(
        adnIdOpt = nodeIdOpt
      )
    } else {
      sd1Common
    }

    // Собрать итоговые данные состояния.
    sd0.copy(
      common = sd2Common,
      nav    = sd1Nav,
      search = sd1Search,
      focused = mFocSdOpt
    )
  }


  /** Используя распарсенные данные состояния FSM, выбрать initial state этого FSM и применить его. */
  override def _runInitState(sd0Opt: Option[SD]): Future[_] = {
    val nextStateFut = sd0Opt.fold [Future[FsmState]] {
      Future.successful( new GeoScInitState )
    } { sd0 =>
      _stateData = sd0
      // Выполнить переход на новое состояние FSM.
      sd0.common.adnIdOpt.fold[Future[FsmState]] {
        // Есть токены, но id узла не задан. Это возможно при TODO гулянии по координатам вне выдачи конкретного узла.
        Future.successful( new GeoScInitState )

      } { onNodeId =>
        // Задан id узла выдачи, перейти в него.
        val routerFut = SrvRouter.getRouter()
        for (_ <- routerFut) yield {
          new NodeIndex_Get_Wait_State
        }
      }
    }
    for (nextState <- nextStateFut) yield {
      become(nextState)
      null
    }
  }

}
