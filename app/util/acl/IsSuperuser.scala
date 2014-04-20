package util.acl

import play.api.mvc._
import scala.concurrent.Future
import util.PlayMacroLogsImpl
import scala.Some
import play.api.mvc.Result
import controllers.Application.http404Fut
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.MAdnNodeCache
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
object IsSuperuser extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsImpl {
  import LOGGER._
  
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) if pw.isSuperuser =>
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        trace(s"for user ${pw.personId} :: ${request.method} ${request.path}")
        sioReqMdFut flatMap { srm =>
          block(RequestWithPwOpt(pwOpt, request, srm))
        }

      case _ => onUnauthFut(request, pwOpt)
    }
  }

  def onUnauthFut(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
    import request._
    warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=$pwOpt")
    http404Fut(request)
  }

}

/**
 * Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
 * @param adnId
 */
case class IsSuperuserAdnNode(adnId: String) extends ActionBuilder[AbstractRequestForAdnNodeAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForAdnNodeAdm[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper.isSuperuser(pwOpt)) {
      val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
      MAdnNodeCache.getByIdCached(adnId) flatMap {
        case Some(adnNode) =>
          sioReqMdFut flatMap { srm =>
            block(RequestForAdnNodeAdm(adnNode, request, pwOpt, srm))
          }
        case None =>
          Future successful Results.NotFound("Adn node not found: " + adnId)
      }
    } else {
      IsSuperuser.onUnauthFut(request, pwOpt)
    }
  }
}


