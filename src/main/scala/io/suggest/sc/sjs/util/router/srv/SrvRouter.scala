package io.suggest.sc.sjs.util.router.srv

import io.suggest.sc.sjs.vm.SafeBody
import io.suggest.sc.sjs.vm.layout.JsRouterTag
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

  /**
   * Получить роутер: со страницы или запросить с сервера асинхронно, если на странице отсутствует.
   * @return Фьючерс с роутером.
   */
  def getRouter: Future[routes.type] = {
    val wnd = dom.window : WindowWithRouterSafe
    val routerUndef = UndefOr.undefOr2ops( wnd.jsRoutes )

    if (routerUndef.nonEmpty) {
      // Роутер уже на странице и готов к работе. Такое возможно, если скрипт роутера был загружен до начала исполнения этого модуля.
      Future successful routerUndef.get

    } else {
      // Нет готового js-роутера. Нужно запросить его с сервера.
      val p = Promise[routes.type]()
      // Возможно, код уже запрашивается с сервера, и тогда routes AsyncInit функция будет уже выставлена.
      val asyncInitOpt = UndefOr.undefOr2ops( wnd.sioScJsRoutesAsyncInit )

      /** Функция для исполнения фьючерса. */
      def pSuccessF(): Unit = {
        p success wnd.jsRoutes.get
      }

      val fun: js.Function0[_] = if (asyncInitOpt.isEmpty) {
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

      // Если нет тега jsRouter'а и callback'а тоже, то добавить тег на страницу.
      if (JsRouterTag.find().isEmpty) {
        // Надо собрать и асинхронно запустить запрос к серверу за jsRouter'ом с помощью script-тега:
        val tag = JsRouterTag()
        SafeBody.append(tag)
      }

      p.future
    }
  }

}
