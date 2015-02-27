package io.suggest.xadv.ext.js.vk.c.hi

import io.suggest.xadv.ext.js.runner.m.ex.LoginApiException
import io.suggest.xadv.ext.js.vk.c.low._
import io.suggest.xadv.ext.js.vk.m.{VkInitOptions, VkLoginResult}

import scala.concurrent.{Future, Promise}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 10:07
 * Description: Hi-level надстройка над слишком низкоуровневым и примитивным API вконтакта.
 */
object Vk {
  def Api = VkApi
  def Auth = VkApiAuth

  // TODO Задействовать runNow? Это ускорит init().
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  /**
   *  Асинхронный запрос инициализация. Внутри по факту синхронный код.
   * @param opts Настройки клиента.
   * @return Фьчерс.
   */
  def init(opts: VkInitOptions): Future[_] = {
    Future {
      VkLow.init(opts)
    }
  }
}


object VkApi {
  // TODO wall.post
}


/** Высокоуровневое API для аутентификации. Т.к. это API реализовано на уровне openapi.js, а не на http,
  * то scala-обертки реализуются намного проще. */
object VkApiAuth {

  /**
   * Безопасный враппер вызовов к Auth. Позволяет перевести callback'и на язык scala.concurrent и отрабатывает
   * исключения.
   * @param name Название вызова. Используется в сообщениях об ошибках.
   * @param f Вызов к API.
   * @return Фьючерс с опциональным результатом. None значит, что юзер не залогинен или не добавил приложение на стену.
   */
  protected def wrapped(name: String)(f: Callback => Unit): Future[Option[VkLoginResult]] = {
    val p = Promise[Option[VkLoginResult]]()
    // Слегка защищаемся от проблем при вызове и при парсинге ответа.
    try {
      f { res: JSON =>
        try {
          p success VkLoginResult.maybeFromResp(res)
        } catch {
          case ex: Throwable =>
            p failure LoginApiException(s"Cannot understand VK.Auth.$name() response.", ex)
        }
      }
    } catch {
      case ex: Throwable =>
        p failure LoginApiException(s"Cannot call VK.Auth.$name", ex)
    }
    p.future
  }

  /**
   * Пропедалировать залогиневание юзера.
   * @param accessLevel Уровень доступа.
   * @return Фьючерс с результатами логина.
   */
  def login(accessLevel: Int): Future[Option[VkLoginResult]] = {
    wrapped("login")(VkLow.Auth.login)
  }


  /**
   * Проверить, залогинен ли юзер.
   * @return Фьючерс. None если нет, Some() если уже залогинен и заапрувил приложение.
   */
  def getLoginStatus: Future[Option[VkLoginResult]] = {
    wrapped("getLoginStatus")(VkLow.Auth.getLoginStatus)
  }

}

