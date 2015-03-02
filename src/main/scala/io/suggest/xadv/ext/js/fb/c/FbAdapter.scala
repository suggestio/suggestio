package io.suggest.xadv.ext.js.fb.c

import io.suggest.xadv.ext.js.fb.c.hi.Fb
import io.suggest.xadv.ext.js.fb.m._
import io.suggest.xadv.ext.js.runner.m.ex.{UrlLoadTimeoutException, DomUpdateException, ApiInitException}
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, MJsCtx, IAdapter}
import org.scalajs.dom

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 17:24
 * Description: Адаптер для постинга в facebook.
 */
object FbAdapter {

  /** Таймаут загрузки скрипта. */
  def SCRIPT_LOAD_TIMEOUT = 10000

  /** Ссылка на скрипт. */
  def SCRIPT_URL = "https://connect.facebook.net/en_US/sdk.js"

  /** Добавить тег со скриптом загрузки facebook js sdk. */
  private def addScriptTag(): Unit = {
    val d = dom.document
    val id = "facebook-jssdk"
    if (d.getElementById(id) == null) {
      val tag = d.createElement("script")
      tag.setAttribute("async", true.toString)
      tag.setAttribute("type", "text/javascript")
      tag.setAttribute("src", SCRIPT_URL)
      d.getElementsByTagName("body")(0)
        .appendChild(tag)
    }
  }

}


import FbAdapter._


class FbAdapter extends IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    domain matches "^https?://(www\\.)?facebook.(com|net)/.*"
  }

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  override def ensureReady(mctx0: MJsCtx): Future[MJsCtx] = {
    val p = Promise[MJsCtx]()
    // Подписаться на событие загрузки скрипта.
    val window: FbWindow = dom.window
    window.fbAsyncInit = { () =>
      val appId = mctx0.service.appId.orNull
      val opts = FbInitOptions(appId)
      Fb.init(opts) onComplete {
        // Инициализация удалась.
        case Success(_) =>
          p success mctx0.copy(
            status = Some(MAnswerStatuses.Success)
          )
        // Возник облом при инициализации.
        case Failure(ex) =>
          p failure ApiInitException(ex)
      }
      // Вычищаем эту фунцкию из памяти браузера.
      window.fbAsyncInit = null
    }
    // Добавить скрипт facebook.js на страницу
    try {
      addScriptTag()
    } catch {
      case ex: Throwable =>
        p failure DomUpdateException(ex)
    }
    // Среагировать на слишком долгую загрузку скрипта таймаутом.
    dom.setTimeout(
      {() =>
        if (!p.isCompleted)
          p failure UrlLoadTimeoutException(SCRIPT_URL, SCRIPT_LOAD_TIMEOUT)
      },
      SCRIPT_LOAD_TIMEOUT
    )
    p.future
  }

  /** Запуск обработки одной цели. */
  override def handleTarget(mctx0: MJsCtx): Future[MJsCtx] = ???

}
