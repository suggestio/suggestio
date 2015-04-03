package io.suggest.xadv.ext.js.vk.c

import io.suggest.xadv.ext.js.runner.c.IActionContext
import io.suggest.xadv.ext.js.runner.c.adp.LazyPermsInit
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, MJsCtxT, IMExtTarget}
import io.suggest.xadv.ext.js.vk.c.hi.Vk
import io.suggest.xadv.ext.js.vk.m._
import org.scalajs.dom

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 16:02
 * Description: Инициализатор VK-адаптера и его состояния.
 */
class VkInit(implicit val actx: IActionContext) extends LazyPermsInit {

  override type SvcExtTg_t  = IMExtTarget
  override type LoginRes_t  = Option[VkLoginResult]
  override type InitRes_t   = Unit
  override type Ctx_t       = MJsCtxT
  override type Perm_t      = VkPerm

  /** Маска прав для логина. */
  lazy val PERMS = Set [Perm_t] (VkPerms.Photos, VkPerms.Wall)

  /** Вызов асинхронной инициализации API. */
  override def svcApiInit(appId: String): Future[InitRes_t] = {
    Vk.init( VkInitOptions(appId) )
  }

  /** Запросить текущие данные по залогиненности текущего юзера. */
  override def getLoginStatus(initRes: InitRes_t): Future[LoginRes_t] = {
    Vk.Auth.getLoginStatus recover {
      case ex: Throwable =>
        // Подавляем любые ошибки, т.к. эта функция в общем некритична, хоть и намекает на неработоспособность API.
        dom.console.warn("VK Cannot getLoginStatus(): %s: %s", ex.getClass.getSimpleName, ex.getMessage)
        None
    }
  }

  /** Проверить, подключено ли приложение? */
  override def isAppConnected(loginRes: LoginRes_t): Boolean = {
    loginRes.isDefined
  }

  /** Запросить у юзера новые пермишшены для приложения. */
  override def requestNewPerms(perms: Set[Perm_t]): Future[Seq[Perm_t]] = {
    val bitmask = VkPerms.toBitMask(perms)
    Vk.Auth.login(bitmask)
      .filter { _.isDefined }
      .map { _ => perms.toSeq }   // TODO Надо наверное избегать лишних конверсий?
  }

  /** Извлечь пермишшены из результатов логина. */
  override def getAppPermissionsFromLoginResp(loginRes: LoginRes_t): Seq[Perm_t] = {
    // Мы требуем пермишшены сразу, без ленивости, поэтому выбор не велик:
    if (loginRes.isDefined)
      PERMS.toSeq
    else
      Nil
  }


  /** Узнать текущие пермишшены приложения в рамках юзера. */
  override def getAppPermissionsFromRemote(loginRes: LoginRes_t): Future[Seq[Perm_t]] = {
    Vk.Api.getAppPermissions()
      .map { resp => VkPerms.fromBitMask(resp.bitMask) }
  }

  override def reduceTargetsToPerms(tgts: Seq[SvcExtTg_t]): Set[Perm_t] = {
    PERMS
  }

  /** Приведение цели к сервисному виду. */
  override def mapTarget(tg: IMExtTarget): Future[SvcExtTg_t] = {
    Future successful tg
  }

  /** Залогинить юзера и подключить приложение к аккаунту юзера. */
  override def loginWithOutPerms(): Future[LoginRes_t] = {
    // Тут сразу запрашиваем пермишшены, хоть этого и не требуется.
    val bitmask = VkPerms.toBitMask(PERMS)
    Vk.Auth.login(bitmask)
  }


  /** Запуск инициализатора на выполнение. */
  def main(): Future[Ctx_t] = {
    val tgtsFut = newTargetsFut
    val lsFut = loginStatusFut
    for {
      //_     <- hasPerms2Fut
      tgts  <- tgtsFut
      lsOpt <- lsFut
    } yield {
      actx.mctx0.copy(
        status = Some( MAnswerStatuses.Success ),
        custom = Some( VkCtx(login = lsOpt).toJson ),
        svcTargets = tgts
      )
    }
  }

}
