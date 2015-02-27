package io.suggest.xadv.ext.js.vk.c

import io.suggest.xadv.ext.js.runner.m.ex.{ApiInitException, LoginCancelledException, DomUpdateException, UrlLoadTimeoutException}
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, IAdapter, MJsCtx}
import io.suggest.xadv.ext.js.vk.c.hi.Vk
import io.suggest.xadv.ext.js.vk.c.low.VkLow
import io.suggest.xadv.ext.js.vk.m.{VkCtx, VkWindow, VkLoginResult, VkInitOptions}
import org.scalajs.dom
import io.suggest.xadv.ext.js.vk.m.VkWindow._

import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 15:10
 * Description: Клиент-адаптер для вконтакта.
 */

object VkAdapter {

  /** bitmask разрешений на доступ к API. */
  private def ACCESS_LEVEL = 8197

  /** Относится ли указанный домен к текущему клиенту? */
  def isMyDomain(domain: String): Boolean = {
    domain.matches("(www\\.)?vk(ontakte)?\\.(ru|com)")
  }

  private def SCRIPT_LOAD_TIMEOUT_MS = 10000

  private def SCRIPT_URL = "https://vk.com/js/api/openapi.js"

  /** Добавить необходимые теги для загрузки. Максимум один раз. */
  private def addOpenApiScript(): Unit = {
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
    el.setAttribute("src", SCRIPT_URL)
    el.setAttribute("async", true.toString)
    dom.document
      .getElementById(divName)
      .appendChild(el)
  }
}


import VkAdapter._


class VkAdapter extends IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  override def isMyDomain(domain: String): Boolean = {
    VkAdapter.isMyDomain(domain)
  }

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу. */
  override def ensureReady(mctx0: MJsCtx): Future[MJsCtx] = {
    val p = Promise[MJsCtx]()
    // Создать обработчик событие инициализации.
    val window: VkWindow = dom.window
    window.vkAsyncInit = {() =>
      val apiId = mctx0.service.appId.orNull
      val opts = VkInitOptions(apiId)
      Vk.init(opts).flatMap { _ =>
        // Начальная инициализация vk openapi.js вроде бы завершена. Можно узнать на тему залогиненности клиента.
        Vk.Auth.getLoginStatus
      } onComplete {
        // init завершился, инфа по залогиненности получена.
        case Success(loginStatusOpt) =>
          p success mctx0.copy(
            status = Some(MAnswerStatuses.Success),
            custom = loginStatusOpt.map(_.toJson)
          )
        // Какая-то из двух операций не удалась. Не важно какая -- суть одна: api не работает.
        case Failure(ex) =>
          p failure ApiInitException(ex)
      }
      // Освободить память браузера от хранения этой функции.
      window.vkAsyncInit = null
    }
    // Добавить тег со ссылкой на open-api. Это запустит процесс в фоне.
    Future {
      addOpenApiScript()
    } onFailure { case ex =>
      p failure DomUpdateException(ex)
    }
    // Отрабатываем таймаут загрузки скрипта вконтакта
    dom.setTimeout(
      {() =>
        // js однопоточный, поэтому никаких race conditions между двумя нижеследующими строками тут быть не может:
        if (!p.isCompleted)
          p failure UrlLoadTimeoutException(SCRIPT_URL, SCRIPT_LOAD_TIMEOUT_MS)
      },
      SCRIPT_LOAD_TIMEOUT_MS
    )
    // Вернуть фьючерс результата
    p.future
  }



  /** Запуск обработки одной цели. */
  override def handleTarget(mctx0: MJsCtx): Future[MJsCtx] = {
    loggedIn [MJsCtx] (VkCtx.maybeFromDyn(mctx0.custom)) { vkCtx =>
      if (mctx0.mads.headOption.flatMap(_.picture).flatMap(_.saved).isDefined) {
        // Публикация идёт в два шага: загрузка картинки силами сервера s.io и публикация записи с картинкой.
        dom.console.log("vk.handleTarget() picture already uploaded. publishing.")
        ???
      } else if (mctx0.mads.nonEmpty) {
        dom.console.log("vk.handleTarget(): Requesing s2s pic upload.")
        ???
      } else {
        // TODO Should never happen. Нет карточек для размещения.
        ???
      }
    }
  }

  protected def runLogin(): Future[VkLoginResult] = {
    Vk.Auth.login(ACCESS_LEVEL) flatMap {
      case None =>
        Future failed LoginCancelledException()
      case Some(login) =>
        Future successful login
    }
  }

  /**
   * Произвести вызов указанного callback'a, предварительно убедившись, что юзер залогинен.
   * @param vkCtxOpt Исходный контекст.
   * @param f callback. Вызывается когда очевидно, что юзер залогинен.
   * @tparam T Тип результата callback'а и этого метода.
   * @return Фьючерс с результатом callback'а или ошибкой.
   */
  protected def loggedIn[T](vkCtxOpt: Option[VkCtx])(f: VkCtx => Future[T]): Future[T] = {
    val loginOpt = vkCtxOpt.flatMap(_.login)
    if (loginOpt.isEmpty) {
      // Юзер не залогинен.
      runLogin().flatMap { loginCtx =>
        val ctx1 = vkCtxOpt match {
          case Some(vkCtx) =>
            // Залить новую инфу по логину во внутренний контекст
            vkCtx.copy(login = Some(loginCtx))
          case None =>
            // should never happen
            VkCtx(login = Some(loginCtx))
        }
        f(ctx1)
      }

    } else {
      // Юзер залогинен уже. Сразу дергаем callback.
      f(vkCtxOpt.get)
    }
  }

}
