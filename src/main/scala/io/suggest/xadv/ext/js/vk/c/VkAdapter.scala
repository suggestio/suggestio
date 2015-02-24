package io.suggest.xadv.ext.js.vk.c

import java.util.concurrent.TimeoutException
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, IAdapter, MJsCtx}
import io.suggest.xadv.ext.js.vk.m.VkInitOptions
import org.scalajs.dom
import io.suggest.xadv.ext.js.vk.m.VkWindow._

import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 15:10
 * Description: Клиент-адаптер для вконтакта.
 */

class VkAdapter extends IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    domain.matches("(www\\.)?vk(ontakte)?\\.(ru|com)")
  }

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  override def ensureReady(mctx0: MJsCtx): Future[MJsCtx] = {
    val p = Promise[MJsCtx]()
    // Создать обработчик событие инициализации.
    dom.window.vkAsyncInit = {() =>
      val apiId = mctx0.service.appId.orNull
      dom.console.log("vkAsyncInit() called. apiId = " + apiId)
      val opts = VkInitOptions(apiId)
      VK.init(opts)
      // Начальная инициализация vk openapi.js вроде бы завершена. Пора запустить возврат результата.
      p success mctx0.copy(
        status = Some(MAnswerStatuses.Success)
      )
    }
    // Добавить тег со ссылкой на open-api. Это запустит процесс в фоне.
    Future {
      addOpenApiScript()
    } onFailure { case ex =>
      dom.console.error("Failed to add/load vk openapi.js script: " + ex.getClass + " " + ex.getMessage)
      p failure ex
    }
    // Отрабатываем таймаут загрузки скрипта вконтакта
    dom.setTimeout(
      {() =>
        // js однопоточный, поэтому никаких race conditions между двумя нижеследующими строками тут быть не может:
        if (!p.isCompleted)
          p failure new TimeoutException("Failed to load script")
      },
      10000
    )
    // Вернуть фьючерс результата
    p.future
  }

  /** Добавить необходимые теги для загрузки. Максимум один раз. */
  def addOpenApiScript(): Unit = {
    // Создать div для добавления туда скрипта. Так зачем-то было сделано в оригинале.
    val div = dom.document.createElement("div")
    val divName = "vk_api_transport"
    div.setAttribute("id", divName)
    dom.document
      .getElementsByTagName("body")(0)
      .appendChild(div)

    // Создать тег скрипта и добавить его в свежесозданный div
    val el = dom.document.createElement("script")
    el.setAttribute("type",  "text/javascript")
    // Всегда долбимся на https. Это работает без проблем с file:///, а на мастере всегда https.
    el.setAttribute("src", "https://vk.com/js/api/openapi.js")
    dom.document
      .getElementById(divName)
      .appendChild(el)
  }

}
