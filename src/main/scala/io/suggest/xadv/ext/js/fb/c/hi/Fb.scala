package io.suggest.xadv.ext.js.fb.c.hi

import io.suggest.sjs.common.model.{ToJsonDictDummy, IToJsonDict, FromJsonT}
import io.suggest.xadv.ext.js.fb.c.low.FbLow
import io.suggest.xadv.ext.js.fb.m._
import io.suggest.xadv.ext.js.runner.m.ex.{ApiException, LoginApiException}

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 18:01
 * Description: Высокоуровневая обвязка над facebook js API. Скрывает всю сериализацию-десериализацию и прочее.
 */

object Fb {

  private def HTTP_GET   = "GET"
  private def HTTP_POST  = "POST"

  /**
   * Высокоуровневая асинхронная инициализация facebook js API.
   * Используется Future{} для упрощения обработки ошибок при асинхронной инициализации.
   * @param opts Экземпляр параметров инициализации.
   * @return Фьчерс
   */
  def init(opts: FbInitOptions)(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      FbLow.init(opts.toJson)
    }
  }

  /**
   * Враппер для вызова login-функций.
   * @param f Функция, вызывающая login-логику FbLow.
   *          Первый аргумент этой функции -- callback-функция.
   * @return Фьючерс с результатом.
   */
  protected def _loginWrapper(f: (Function[Dictionary[Any], _]) => Unit): Future[FbLoginResult] = {
    val p = Promise[FbLoginResult]()
    try {
      f { resp: Dictionary[Any] =>
        try {
          p success FbLoginResult.fromLoginResp(resp)
        } catch {
          case ex: Throwable =>
            p failure LoginApiException("Failed to process FB.login() result.", ex)
        }
      }
    } catch {
      case ex: Throwable =>
        p failure LoginApiException("Failed to call FB.login().", ex)
    }
    p.future
  }

  /**
   * Запустить процедура залогинивания юзера в пейсбук.
   * @param args Параметры логина.
   * @return Future.successful с результатом успешного логина.
   *         Future.failed если не удалось запустить логин или распарсить результат.
   */
  def login(args: FbLoginArgs): Future[FbLoginResult] = {
    _loginWrapper {
      FbLow.login(_, args.toJson)
    }
  }


  /**
   * Асинхронно узнать/запросить инфу по текущей залогиненности юзера.
   * @param force Форсировать запрос с fb-сервера, игнорируя закешированные результаты?
   * @return Тоже, что и login().
   */
  def getLoginStatus(force: Boolean = false): Future[FbLoginResult] = {
    _loginWrapper {
      FbLow.getLoginStatus(_, force)
    }
  }

  /**
   * Синхронно получить объект с данными по недавнему вызову login() / getLoginStatus().
   * @return Опциональный распарсенный результат работы.
   *         При ошибках будет exception.
   */
  def getAuthResponse(): Option[FbAuthResponse] = {
    Option( FbLow.getAuthResponse() )
      .map { FbAuthResponse.fromJson }
  }


  /** Высокоуровневый вызов к API. */
  protected def apiCallSafe[T1](httpMethod: String, path: String, args: IToJsonDict,
                                model: FromJsonT { type T = T1 } ): Future[T1] = {
    val p = Promise[T1]()
    try {
      FbLow.api(
        path        = path,
        httpMethod  = httpMethod,
        args        = args.toJson,
        callback    = {resp: Dictionary[Any] =>
          try {
            p success model.fromJson(resp)
          } catch {
            case ex: Throwable =>
              p failure ApiException(s"$httpMethod $path result", ex, Some(resp))
          }
        }
      )
    } catch {
      case ex: Throwable =>
        p failure ApiException(s"$httpMethod $path call", ex, None)
    }
    p.future
  }


  def mkPost(fbTg: String, args: FbPost) = {
    apiCallSafe(
      httpMethod = HTTP_POST,
      path  = s"/$fbTg/feed",
      args  = args,
      model = FbPostResult
    )
  }


  /** Получить инфу по абстрактному fb-узлу. */
  def getNodeInfo(args: FbNodeInfoArgs): Future[FbNodeInfoResult] = {
    apiCallSafe(
      httpMethod  = HTTP_GET,
      path        = args.toPath,
      args        = new ToJsonDictDummy,
      model       = FbNodeInfoResult
    )
  }

  /** Получить инфу по выставленным пермишшенам приложения у юзера. */
  def getPermissions(args: FbGetPermissionsArgs): Future[FbGetPermissionsResult] = {
    var reqPath: String = "/" + args.userId + "/permissions"
    if (args.accessToken.nonEmpty)
      reqPath = reqPath + "?access_token=" + args.accessToken.get
    apiCallSafe(
      httpMethod  = HTTP_GET,
      path        = reqPath,
      args        = new ToJsonDictDummy,
      model       = FbGetPermissionsResult
    )
  }

}
