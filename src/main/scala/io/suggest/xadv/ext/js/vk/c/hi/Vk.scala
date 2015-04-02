package io.suggest.xadv.ext.js.vk.c.hi

import io.suggest.xadv.ext.js.runner.m.{IToJsonDict, FromJsonT}
import io.suggest.xadv.ext.js.runner.m.ex.{ApiException, LoginApiException}
import io.suggest.xadv.ext.js.vk.c.low._
import io.suggest.xadv.ext.js.vk.m._
import org.scalajs.dom

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
  def init(opts: VkInitOptions): Future[Unit] = {
    Future {
      VkLow.init(opts.toJson)
    }
  }
}


/** Безопасные обертки над vk js http-API. */
object VkApi {

  /**
   * Враппер для производства вызова к vk api.
   * @param method Вызываемый метод.
   * @param args Параметры вызова.
   * @param model Модель, занимающаяся десериализацией результатов.
   * @tparam T1 Тип результата.
   * @return Фьючерс с результатом.
   */
  protected def mkCall[T1](method: String, args: IToJsonDict, model: FromJsonT { type T = T1 }): Future[T1] = {
    val p = Promise[T1]()
    try {
      val argsJson = args.toJson
      VkLow.Api.call(method, argsJson, { resp: JSON =>
        try {
          val res = model.fromJson(resp)
          p success res
        } catch {
          case ex: Throwable =>
            p failure ApiException(method, ex, Some(resp))
        }
      })
      dom.console.info("Asynced VK.Api.call(", method, ",", argsJson, ") |", model.getClass.getName)
    } catch {
      case ex: Throwable => p failure ApiException(method, ex, None)
    }
    p.future
  }

  /**
   * Отрезолвить имя во внутренний id.
   * @param args Параметры вызова.
   * @see [[https://vk.com/dev/utils.resolveScreenName]]
   * @return Фьючерс с результатом вызова.
   */
  def resolveScreenName(args: VkResolveScreenNameArgs): Future[VkTargetInfo] = {
    mkCall("utils.resolveScreenName", args, VkResolveScreenNameResult)
  }

  /**
   * Получить инфу по группе.
   * @param args Параметры вызова.
   * @return Фьючерс с результатом.
   */
  def groupGetById(args: VkGroupGetByIdArgs): Future[VkGroupGetByIdResult] = {
    mkCall("groups.getById", args, VkGroupGetByIdResult)
  }

  /**
   * Получить ссылку для загрузки картинки.
   * @param args Аргументы вызова метода.
   * @return Фьючерс с результатом вызова.
   */
  def photosGetWallUploadServer(args: VkPhotosGetWallUploadServerArgs) = {
    mkCall("photos.getWallUploadServer", args, VkPhotosGetWallUploadServerResult)
  }

  /**
   * Присоединить загруженную картинку к стене указанного аккаунта.
   * @param args Аргументы вызова метода.
   * @return Фьючес с результатом вызова.
   */
  def saveWallPhoto(args: VkSaveWallPhotoArgs) = mkCall("photos.saveWallPhoto", args, VkSaveWallPhotoResult)

  /**
   * Постинг описаного сообщения на стену указанного vk-объекта.
   * @param args Параметры вызова.
   * @return Фьючерс с результом вызова.
   */
  def wallPost(args: VkWallPostArgs) = mkCall("wall.post", args, VkWallPostResult)

  /**
   * Запросить текущие разрешения приложения.
   * @param args Параметры вызова.
   * @return Фьючерс с результатом вызова.
   */
  def getAppPermissions(args: VkGetAppPermissionsArgs = VkGetAppPermissionsArgs()) = {
    mkCall("account.getAppPermissions", args, VkGetAppPermissionsResult)
  }
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
    wrapped("login")(VkLow.Auth.login(_, accessLevel))
  }


  /**
   * Проверить, залогинен ли юзер.
   * @return Фьючерс. None если нет, Some() если уже залогинен и заапрувил приложение.
   */
  def getLoginStatus: Future[Option[VkLoginResult]] = {
    wrapped("getLoginStatus")(VkLow.Auth.getLoginStatus)
  }

}

