package io.suggest.xadv.ext.js.fb.c

import io.suggest.xadv.ext.js.fb.c.hi.Fb
import io.suggest.xadv.ext.js.fb.m._
import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.c.adp.LazyPermsInit
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, IMExtTarget}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 15:39
 * Description: Реализация системы инициализации [[FbAdapter]]'а.
 */

class FbInit(implicit val actx: IActionContext) extends LazyPermsInit {

  override type Ctx_t = FbJsCtxT
  override type InitRes_t = Unit

  /** Вызов асинхронной инициализации API. */
  override def svcApiInit(appId: String): Future[InitRes_t] = {
    Fb.init( FbInitOptions(appId) )
  }

  override type LoginRes_t = FbLoginResult

  /** Запросить текущие данные по залогиненности текущего юзера. */
  override def getLoginStatus(initRes: InitRes_t): Future[LoginRes_t] = {
    Fb.getLoginStatus()
  }

  /** Проверить, подключено ли приложение? */
  override def isAppConnected(loginRes: LoginRes_t): Boolean = {
    loginRes.status.isAppConnected
  }

  /** Залогинить юзера и подключить приложение к аккаунту юзера. */
  override def loginWithOutPerms(): Future[LoginRes_t] = {
    val args = FbLoginArgs(
      returnScopes  = Some(true)
    )
    Fb.login(args)
  }

  override type Perm_t = FbPermission

  /** Узнать текущие пермишшены приложения в рамках юзера. */
  override def getAppPermissionsFromRemote(ls: LoginRes_t): Future[Seq[Perm_t]] = {
    val uiatOpt = for {
      authResp  <- ls.authResp
      userId    <- authResp.userId
      atok      <- authResp.accessToken
    } yield {
      (userId, atok)
    }
    // Возможен NoSuchElementException - это нормально.
    val (userId, atok) = uiatOpt.get
    // Если есть текущий токен и userId, то воспользоваться ими для получения списка текущих прав.
    val permArgs = FbGetPermissionsArgs(userId = userId, accessToken = Some(atok))
    Fb.getPermissions(permArgs)
      .map { resp =>
        resp.data
          .iterator
          .filter { _.status.isGranted }    // Нужны только заапрувленные права
          .map { _.permission }             // Отбросить состояние права, оставить только само право.
          .toSeq
      }
  }

  /** Извлечь пермишшены из результатов логина. */
  override def getAppPermissionsFromLoginResp(loginRes: LoginRes_t): Seq[Perm_t] = {
    loginRes.authResp.get.grantedScopes
  }

  override type SvcExtTg_t = FbExtTarget

  /** Приведение цели к сервисному виду. */
  override def mapTarget(tg: IMExtTarget): Future[SvcExtTg_t] = {
    FbUtil.detectTgNodeType(tg)
      .map { FbExtTarget(tg, _) }
  }

  override def reduceTargetsToPerms(tgts: Seq[SvcExtTg_t]): Set[Perm_t] = {
    tgts.iterator
      .flatMap { _.fbTgUnderlying.nodeType }
      .flatMap { _.publishPerms }
      .toSet
  }

  /** Запросить у юзера новые пермишшены для приложения. */
  override def requestNewPerms(perms: Set[Perm_t]): Future[Seq[Perm_t]] = {
    val args = FbLoginArgs(
      scopes        = perms,
      returnScopes  = Some(true),
      authType      = Some(FbAuthTypes.ReRequest)
    )
    Fb.login(args)
      .map { _.authResp.fold [Seq[FbPermission]] (Nil) (_.grantedScopes) }
  }


  /** Сборка кастомного контекста. */
  def customCtx = {
    hasPerms2Fut map { hasPerms =>
      FbCtx(hasPerms = hasPerms)
    }
  }

  /** Запускалка всего вышеперечисленного. */
  def main(): Future[Ctx_t] = {
    // Формируем и возвращаем результирующий внутренний контекст.
    val tgtsFut = newTargetsFut
    for {
      _fbCtx  <- customCtx
      tgts    <- tgtsFut
    } yield {
      new FbJsCtxT {
        override def jsCtxUnderlying  = actx.mctx0
        override def fbCtx            = _fbCtx
        override def status           = Some(MAnswerStatuses.Success)
        override def svcTargets       = tgts
      }
    }
  }

}
