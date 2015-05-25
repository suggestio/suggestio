package io.suggest.sc.sjs.util.router.srv

import io.suggest.sjs.common.view.safe.doc.SafeDocument
import org.scalajs.dom

import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:19
 * Description: Доступ к роутеру осуществляется через эту прослойку.
 * Код js-роутера может быть отрендерен прямо на странице, или может быть запрошен с сервера
 * путем добавления тега на соотв. script на страницу.
 */
object SrvRouter {

  /** Путь из web21/conf/routes к ресурсу js-роутера контроллера выдачи. */
  private def ROUTER_URI = "/sc/router.js"

  /**
   * Получить роутер: со страницы или запросить с сервера асинхронно, если на странице отсутствует.
   * @return Фьючерс с роутером.
   */
  def getRouter: Future[routes.type] = {
    val wnd = dom.window : WindowWithRouterSafe
    val routerUndef = UndefOr.undefOr2ops( wnd.jsRoutes )
    if (routerUndef.nonEmpty) {
      Future successful routerUndef.get

    } else {
      // Нет готового js-роутера. Нужно запросить его с сервера.
      val p = Promise[routes.type]()
      // Возможно, код уже запрашивается с сервера, и тогда routes AsyncInit функция будет уже выставлена.
      val asyncInitOpt = UndefOr.undefOr2ops( wnd.sioScJsRoutesAsyncInit )
      def pSuccessF(): Unit = {
        p success wnd.jsRoutes.get
      }
      val fun: js.Function0[_] = if (asyncInitOpt.isEmpty) {
        // Надо собрать и асинхронно запустить запрос к серверу с помощью script-тега, добавленного в конец head:
        val scriptEl = dom.document.createElement("script")
        scriptEl.setAttribute("async", true.toString)
        scriptEl.setAttribute("type", "text/javascript")
        scriptEl.setAttribute("src", ROUTER_URI)
        SafeDocument()
          .head
          .appendChild(scriptEl)

        // Вернуть callback для скрипта роутера в текущем потоке:
        {() =>
          pSuccessF()
          // Callback-функция инициализации больше не требуется. Освободить память браузера от нее.
          wnd.sioScJsRoutesAsyncInit = js.undefined
        }

      } else {
        // Уже кто-то ожидает инициализации. Добавлять script-тег не требуется, т.к. он должен быть уже добавлен.
        val prevFun = { asyncInitOpt.get }
        {() =>
          pSuccessF()
          prevFun()
        }
      }
      // TODO Выставить таймаут ожидания ответа сервера и другие детекторы ошибок?
      wnd.sioScJsRoutesAsyncInit = fun

      p.future
    }
  }

}
