package io.suggest.sc.view.jsrouter

import io.suggest.routes.routes
import io.suggest.sjs.common.vm.doc.SafeBody

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

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
  def ensureJsRouter(): Future[routes.type] = {
    if (GlobalScRouterSafe.jsRoutes.nonEmpty) {
      // Роутер уже на странице и готов к работе. Такое возможно, если скрипт роутера был загружен до начала исполнения этого модуля.
      Future.successful( GlobalScRouterSafe.jsRoutes.get )

    } else {
      // Нет готового js-роутера. Нужно запросить его с сервера.
      val p = Promise[routes.type]()
      // Возможно, код уже запрашивается с сервера, и тогда routes AsyncInit функция будет уже выставлена.
      val asyncInitOpt = GlobalScRouterSafe.sioScJsRoutesAsyncInit

      /** Функция для исполнения фьючерса. */
      def pSuccessF(): Unit = {
        for ( jsRouter <- GlobalScRouterSafe.jsRoutes if !p.isCompleted )
          p.success( jsRouter )
      }

      val fun: js.Function0[_] = asyncInitOpt.fold[js.Function0[_]] {
        // Вернуть callback для скрипта роутера в текущем потоке:
        {() =>
          pSuccessF()
          // Callback-функция инициализации больше не требуется. Освободить память браузера от нее.
          GlobalScRouterSafe.sioScJsRoutesAsyncInit = js.undefined
        }
      } { prevFun =>
        // Уже кто-то ожидает инициализации. Добавлять script-тег не требуется, т.к. он должен быть уже добавлен.
        {() =>
          pSuccessF()
          prevFun()
        }
      }

      // TODO Выставить таймаут ожидания ответа сервера и другие детекторы ошибок?
      GlobalScRouterSafe.sioScJsRoutesAsyncInit = fun

      // Если нет тега jsRouter'а, то добавить тег на страницу.
      if (JsRouterTag.find().isEmpty) {
        // Надо собрать и асинхронно запустить запрос к серверу за jsRouter'ом с помощью script-тега:
        val tag = JsRouterTag()
        SafeBody.append(tag)
      }

      p.future
    }
  }

}
