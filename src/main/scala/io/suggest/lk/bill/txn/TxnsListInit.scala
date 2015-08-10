package io.suggest.lk.bill.txn

import io.suggest.lk.router.jsRoutes
import org.scalajs.dom
import io.suggest.bill.TxnsListConstants._
import io.suggest.sjs.common.controller.{IInit, InitRouter}
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.jquery.{JQueryAjaxSettings, JQueryEventObject, JQueryXHR, jQuery}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 11:31
 * Description: js для страницы личного кабинета со списком транзакций.
 */
trait TxnsListInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.BillTxnsList) {
      Future {
        new TxnList().init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Весь код инициализации страницы живет в этом классе. */
sealed class TxnList extends IInit with SjsLogger {

  /** Запуск инициализации текущего модуля. */
  override def init(): Unit = {
    initTxnsList()
  }

  /** Выполнить инициализацию страницы со списком транзакций. */
  private def initTxnsList(): Unit = {
    val btn = jQuery("#" + GET_MORE_TXNS_BTN_ID)
    btn.on("click", { (e: JQueryEventObject) =>
      e.preventDefault()
      // Узнаем id узла.
      val adnId = jQuery("#" + ADN_ID_INPUT_ID)
        .`val`()
        .toString
      // Узнаем текущий и следующий номера страниц.
      val currPageInput = jQuery("#" + CURR_PAGE_NUM_INPUT_ID)
      val currPage = currPageInput.`val`()
        .toString
        .toInt
      val nextPage = currPage + 1
      // Узнаем ссылку для ajax-запроса.
      val route = jsRoutes.controllers.MarketLkBilling.txnsList(adnId, nextPage, inline = true)

      // Собрать и запустить ajax-запрос:
      val ajaxSettingsJson = Dictionary[Any](
        "method"  -> route.method,
        "url"     -> route.url,
        "success" -> {(data: Any, textStatus: String, jqXHR: JQueryXHR) =>
          // Залить верстку полученного списка транзакций в таблицу.
          jQuery("#" + TXNS_CONTAINER_ID)
            // TODO Тут нужна анимация. Надо что-то типа slideDown()
            .append(data)
          // Если есть ещё транзакции на сервере...
          val hasMore = Option( jqXHR.getResponseHeader(HAS_MORE_TXNS_HTTP_HDR) )
            .filter { !_.isEmpty }
            .fold(true)(_.toBoolean)
          if (hasMore) {
            currPageInput.`val`(nextPage.toString)
          } else {
            // Больше нет транзакций на сервере, кнопка больше не нужна.
            val durationMs = 333
            btn.hide(durationMs)
            dom.setTimeout(
              {() => btn.remove()},
              durationMs + 100
            )
          }
        },
        "error" -> {(jqXHR: JQueryXHR, textStatus: String, errorThrow: String) =>
          error("Cannot download more transactions: " + errorThrow + ": " + jqXHR.status + " " + jqXHR.statusText + "\n " + jqXHR.responseText)
        }
      )
      val ajaxSettings = ajaxSettingsJson.asInstanceOf[JQueryAjaxSettings]
      jQuery.ajax(ajaxSettings)
    })
  }

}
