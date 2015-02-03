package util.acl

import models.usr.{EmailActivation, EmailPwIdent}
import util._
import play.api.mvc._
import util.ident.IdentUtil
import views.html.ident._, recover._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import SiowebEsUtil.client
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:08
 * Description:
 * ActionBuilder для некоторых экшенов восстановления пароля. Завязан на некоторые фунции контроллера, поэтому
 * лежит здесь.
 */

object CanRecoverPw
  extends BruteForceProtectSimple
  with PlayMacroLogsImpl


trait CanRecoverPwBase extends ActionBuilder[RecoverPwRequest] {

  import CanRecoverPw._

  def eActId: String

  protected def keyNotFound(implicit request: RequestHeader): Future[Result] = {
    MarketIndexAccess.getNodes.map { nodes =>
      implicit val ctx = new ContextImpl
      Results.NotFound(pwRecoverFailedTpl(nodes))
    }
  }

  override def invokeBlock[A](request: Request[A], block: (RecoverPwRequest[A]) => Future[Result]): Future[Result] = {
    lazy val logPrefix = s"CanRecoverPw($eActId): "
    bruteForceProtectedNoimpl(request) {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      EmailActivation.getById(eActId).flatMap {
        case Some(eAct) =>
          EmailPwIdent.getById(eAct.email) flatMap {
            case Some(epw) if epw.personId == eAct.key =>
              // Можно отрендерить блок
              LOGGER.debug(logPrefix + "ok: " + request.path)
              srmFut flatMap { srm =>
                val req1 = RecoverPwRequest(request, pwOpt, epw, eAct, srm)
                block(req1)
              }
            // should never occur: Почему-то нет парольной записи для активатора.
            // Такое возможно, если юзер взял ключ инвайта в маркет и вставил его в качестве ключа восстановления пароля.
            case None =>
              LOGGER.error(s"${logPrefix}eAct exists, but emailPw is NOT! Hacker? pwOpt = $pwOpt ;; eAct = $eAct")
              keyNotFound(request)
          }
        case None =>
          pwOpt match {
            // Вероятно, юзер повторно перешел по ссылке из письма.
            case Some(pw) =>
              IdentUtil.redirectUserSomewhere(pw.personId)
            // Юзер неизвестен и ключ неизвестен. Возможно, перебор ключей какой-то?
            case None =>
              LOGGER.warn(logPrefix + "Unknown eAct key. pwOpt = " + pwOpt)
              keyNotFound(request)
          }
      }
    }
  }
}


/**
 * Дефолтовая реализация [[CanRecoverPwBase]].
 * @param eActId id ключа активации.
 */
case class CanRecoverPw(eActId: String)
  extends CanRecoverPwBase


/** Реквест активации. */
case class RecoverPwRequest[A](
  request   : Request[A],
  pwOpt     : PwOpt_t,
  epw       : EmailPwIdent,
  eAct      : EmailActivation,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)


