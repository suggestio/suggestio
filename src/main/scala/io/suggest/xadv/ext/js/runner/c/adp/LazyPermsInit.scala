package io.suggest.xadv.ext.js.runner.c.adp

import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.m.{IMExtTarget, MJsCtxT}
import io.suggest.xadv.ext.js.runner.m.ex.LoginCancelledException
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.runNow

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 14:56
 * Description: Каркас для асинхронной инициализации адаптера, API сервиса и состояния адаптера по сценарию:
 * 0. Svc.Api.init()
 * 1. Убедится, что юзер залогинен.
 * 2. Узнать текущие пермишшены.
 * 3. Собрать инфу по целям, если требуется.
 * 3. Узнать необходиые пермишшены на основе обработанных целей.
 * 4. Запросить у юзера недостающие пермишшены.
 * 5. Инициализировать контекст и вернуть его.
 *
 * Подобный сценарий работы свойственен для OAuth2-приложений.
 */

trait LazyPermsInit {

  /** Контекст экшена. */
  implicit def actx: IActionContext

  /** Тип результата работы. */
  type Ctx_t <: MJsCtxT

  /** В каком порядке исполнять асинхронные операции? */
  implicit def execCtx: ExecutionContext = runNow

  /** Тип значения, возвращаемого при инициализации. */
  type InitRes_t

  /** Вызов асинхронной инициализации API. */
  def svcApiInit(appId: String): Future[InitRes_t]

  /** Фьючерс инициализации. */
  def initFut: Future[InitRes_t] = {
    val appId = actx.mctx0.service
      .flatMap(_.appId)
      .orNull
    svcApiInit(appId)
  }


  /** Тип значения, возвращаемого при login() и при getLoginStatus(). */
  type LoginRes_t

  /** Запросить текущие данные по залогиненности текущего юзера. */
  def getLoginStatus(initRes: InitRes_t): Future[LoginRes_t]

  /** Запрашиваем инфу по fb-залогиненности текущего юзера и права доступа. */
  def loginStatusEarlyFut = initFut flatMap getLoginStatus

  /** Проверить, подключено ли приложение? */
  def isAppConnected(loginRes: LoginRes_t): Boolean

  /** Залогинить юзера и подключить приложение к аккаунту юзера. */
  def loginWithOutPerms(): Future[LoginRes_t]

  /** Если юзер не подключил приложение или не залогинен на сервисе,
    * то надо вызывать окно FB-логина без каких-либо конкретных прав. */
  lazy val loginStatusFut = loginStatusEarlyFut
    .filter { isAppConnected }
    .recoverWith { case ex: Throwable =>
      // Отправляем окно FB-логина в очередь на отображение.
      val res = actx.app.popupQueue
        .enqueue { () => loginWithOutPerms() }
      if (!ex.isInstanceOf[NoSuchElementException])
        dom.console.warn(getClass.getSimpleName + " failed to getLoginStatus(): %s: %s", ex.getClass.getName, ex.getMessage)
      res
    }
    .filter { isAppConnected }
    .recoverWith { case ex: Throwable =>
      if (!ex.isInstanceOf[NoSuchElementException])
        dom.console.warn(getClass.getSimpleName + " failed to Fb.login() w/o perms: %s: %s", ex.getClass.getName, ex.getMessage)
      Future failed LoginCancelledException()
    }

  
  /** Тип пермишшена. */
  type Perm_t

  /** Узнать текущие пермишшены приложения в рамках юзера. */
  def getAppPermissionsFromRemote(loginRes: LoginRes_t): Future[Seq[Perm_t]]

  /** Когда появится инфа о текущем залогиненном юзере, то нужно запросить уже доступные пермишшены. */
  def earlyHasPermsFut: Future[Seq[Perm_t]] = {
    loginStatusFut
      // Анализировать новых юзеров смысла нет
      .filter { isAppConnected }
      // У известного юзера надо извлечь данные о текущих пермишшенах и его fb userID.
      .flatMap { getAppPermissionsFromRemote }
  }


  /** Извлечь пермишшены из результатов логина. */
  def getAppPermissionsFromLoginResp(loginRes: LoginRes_t): Seq[Perm_t]

  /** Получить имеющиеся пермишшены. */
  lazy val hasPermsFut: Future[Seq[Perm_t]] = {
    earlyHasPermsFut
      .recoverWith { case ex: NoSuchElementException =>
      loginStatusFut
        .map { getAppPermissionsFromLoginResp }
    }
  }


  /** Тип контекстной цели. */
  type SvcExtTg_t <: IMExtTarget
  
  /** Приведение цели к сервисному виду. */
  def mapTarget(tg: IMExtTarget): Future[SvcExtTg_t]

  /** Параллельно вычисляем данные по целям. */
  def newTargetsFut = loginStatusFut.flatMap { _ =>
    Future.traverse( actx.mctx0.svcTargets )(mapTarget)
  }
  
  def reduceTargetsToPerms(tgts: Seq[SvcExtTg_t]): Set[Perm_t]
  
  /** Вычисляем необходимые для целей пермишшены на основе имеющегося списка целей. */
  def tgsWantPermsFut = newTargetsFut map reduceTargetsToPerms

  /** Вычисляем недостающие пермишшены, которые нужно запросить у юзера. */
  def needPermsFut: Future[Set[Perm_t]] = {
    for {
      hasPerms      <- hasPermsFut
      tgsWantPerms  <- tgsWantPermsFut
    } yield {
      val res = tgsWantPerms -- hasPerms
      println("I have perms = " + hasPerms.mkString(",") + " ; tg want perms = " + tgsWantPerms.mkString(",") + " ; so, need perms = " + res.mkString(","))
      res
    }
  }

  /** Запросить у юзера новые пермишшены для приложения. */
  def requestNewPerms(perms: Set[Perm_t]): Future[Seq[Perm_t]]

  /** Отправляем попап логина в очередь на экран для получения новых пермишеннов, если такое требуется. */
  def hasPerms2Fut = needPermsFut
    .filter { _.nonEmpty }
    .flatMap { needPerms =>
      println("Asking for new FB permissions: " + needPerms.mkString(", "))
      actx.app.popupQueue
        .enqueue { () => requestNewPerms(needPerms) }
    }
    .recoverWith {
      case ex: NoSuchElementException =>
        println("Permissing request is not needed. Skipping.")
        hasPermsFut
  }

}
