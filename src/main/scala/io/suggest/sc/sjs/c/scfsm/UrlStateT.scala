package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.ScConstants.ScJsState._
import io.suggest.sc.sjs.vm.SafeWnd

import scala.scalajs.js.URIUtils

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.16 11:40
  * Description: Поддержка связывания состояния ScFsm с URL через History API.
  */
trait UrlStateT extends ScFsmStub {

  /** Управление состояние закинуто в отдельный сингтон, чисто в целях группировки. */
  object UrlStates {

    type AccEl_t  = (String, Any)
    type Acc_t    = List[AccEl_t]

    /** Префикс URL Hash. */
    def URL_HASH_PREFIX = "#!?"

    def getUrlHashAcc: Acc_t = {
      var acc: List[(String, Any)] = Nil
      val sd0 = _stateData

      // Пока пишем generation, но наверное это лучше отключить, чтобы в режиме iOS webapp не было повторов.
      acc ::= GENERATION_FN -> sd0.generation

      // Отработка состояния левой панели.
      val npo = sd0.nav.panelOpened
      if (npo) {
        acc ::= GEO_SCR_OPENED_FN -> npo
      }

      // Отрабатываем состояние правой панели.
      val spo = sd0.search.opened
      if (spo) {
        acc = CAT_SCR_OPENED_FN -> spo ::
          SEARCH_TAB_FN -> sd0.search.currTab.id ::
          acc
      }

      // Отработать focused-выдачу, если она активна.
      for (focSd <- sd0.focused; focAdId <- focSd.currAdId) {
        acc ::= FADS_CURRENT_AD_ID_FN -> focAdId
      }

      // Отработать id текущего узла.
      for (nodeId <- sd0.adnIdOpt) {
        acc ::= ADN_ID_FN -> nodeId
      }

      acc
    }

    /** Сериализация аккамулятора в строку. */
    def acc2queryStringBody(acc: TraversableOnce[AccEl_t]): String = {
      acc.toIterator
        .map { kv =>
          kv.productIterator
            .map { s =>
              URIUtils.encodeURIComponent(s.toString)
            }
            .mkString("=")
        }
        .mkString("&")
    }

    /** Заброс текущего состояния FSM в историю. */
    def pushCurrState(): Unit = {
      // Сериализовать куски текущего состояния в URL.
      for (hApi <- SafeWnd.history) {
        val url = acc2queryStringBody( getUrlHashAcc )
        hApi.pushState(null, "sio", Some(URL_HASH_PREFIX + url))
      }
    }

  }

}
