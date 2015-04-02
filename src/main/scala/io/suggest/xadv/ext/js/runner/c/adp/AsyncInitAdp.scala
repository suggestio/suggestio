package io.suggest.xadv.ext.js.runner.c.adp

import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.m.ex.{UrlLoadTimeoutException, DomUpdateException, ApiInitException}
import io.suggest.xadv.ext.js.runner.m.{MJsCtxT, IAdapter}
import org.scalajs.dom
import org.scalajs.dom.Element

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 10:08
 * Description: Адаптеры обычно инициализируются асинхронно, а именно:
 * - Добавляют script-тег и ожидают загрузки скрипта.
 * - Продолжают асинхронную инициализаю, когда скрипт загружен и дергает window.xAsyncInit().
 * Этот код унифицирует логику реакции на ensureReady() между разными адаптерами, работающими с сервисами,
 * поддерживающими асинхронную инициализацию.
 */
trait AsyncInitAdp extends IAdapter {

  /** Тип контекста. */
  type Ctx_t <: MJsCtxT

  /** Абсолютный URL скрипта. */
  def SCRIPT_URL: String

  /** Используемый execution context можно переопределить здесь. */
  implicit def execCtx: ExecutionContext = runNow

  /**
   * Выставление функции-обработчика, который должен перехватываться подключаемым скриптом.
   * @param handler функция инициализации или null.
   *                null означает, что необходимо удалить обработчик.
   */
  def setInitHandler(handler: () => _): Unit

  /** Эта инициализация вызывается, когда скрипт загружен. */
  def serviceScriptLoaded(implicit actx: IActionContext): Future[Ctx_t]

  /** Таймаут загрузки скрипта. */
  def SCRIPT_LOAD_TIMEOUT_MS: Int = 10000

  /** Сборка тега со скриптом. */
  def createScriptTag(): Element = {
    val tag = dom.document.createElement("script")
    tag.setAttribute("async", true.toString)
    tag.setAttribute("type", "text/javascript")
    tag.setAttribute("src", SCRIPT_URL)
    tag
  }

  /** Добавление указанного (обычно script) тега в DOM. */
  def appendScriptTag(tag: Element): Unit = {
    dom.document
      .getElementsByTagName("body")(0)
      .appendChild(tag)
  }

  /** Собрать и добавить тег скрипта в DOM. */
  def addScriptTag(): Unit = {
    appendScriptTag( createScriptTag() )
  }

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  override def ensureReady(implicit actx: IActionContext): Future[MJsCtxT] = {
    // В этот promise будет закинут результат.
    val p = Promise[MJsCtxT]()
    // Чтобы зафиксировать таймаут загрузки API скрипта сервиса, используется второй promise:
    val scriptLoadP = Promise[Null]()
    // Подписаться на событие загрузки скрипта.
    setInitHandler { () =>
      // Скрипт загружен. Сообщаем об этом контейнеру scriptLoadPromise.
      scriptLoadP success null
      // Запускаем инициализацию.
      val initFut = serviceScriptLoaded
        // Любое исключение завернуть в ApiInitException
        .recoverWith { case ex: Throwable =>
          dom.console.error(getClass.getSimpleName + ": init failed: " + ex.getClass.getName + ": " + ex.getMessage)
          Future failed ApiInitException(ex)
        }
      p completeWith initFut
      // Вычищаем эту функцию из памяти браузера, когда она подходит к концу.
      setInitHandler(null)
    }
    // Добавить скрипт API сервиса на страницу
    try {
      addScriptTag()
    } catch {
      case ex: Throwable =>
        p failure DomUpdateException(ex)
        dom.console.error(getClass.getSimpleName + ": addScriptTag() failed: " + ex.getClass.getName + ": " + ex.getMessage)
    }
    // Среагировать на слишком долгую загрузку скрипта таймаутом.
    val t = SCRIPT_LOAD_TIMEOUT_MS
    dom.setTimeout(
      {() =>
        if (!scriptLoadP.isCompleted) {
          p failure UrlLoadTimeoutException(SCRIPT_URL, t)
          dom.console.error(getClass.getSimpleName + ": timeout %s ms occured during ensureReady() script inject", t)
        }
      },
      t
    )
    p.future
  }

}
