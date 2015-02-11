package util.acl

import models.{MAdnNodeCache, MAdnNode}
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:37
 * Description: ACL для проверки доступа к узлу (если id передан), но при отсуствии доступа просто нет узла в реквесте.
 * sioReqMd отрабатывается аналогично.
 * Появилось для lkList, где по дизайну было наличие текущей ноды, но для шаблона это было необязательно.
 */

trait IsAdnNodeAdminOptOrAuthBase extends ActionBuilder[RequestWithNodeOpt] with PlayMacroLogsDyn {

  /** id узла, если есть. */
  def adnIdOpt: Option[String]

  override def invokeBlock[A](request: Request[A], block: (RequestWithNodeOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) =>
        MAdnNodeCache.maybeGetByIdCached(adnIdOpt).flatMap { mnodeOpt =>
          val mnodeOpt1 = mnodeOpt.filter { mnode =>
            IsAdnNodeAdmin.isAdnNodeAdminCheck(mnode, pwOpt)
          }
          val srmFut: Future[SioReqMd] = mnodeOpt1 match {
            case Some(mnode) =>
              SioReqMd.fromPwOptAdn(pwOpt, mnode.id.get)
            case other =>
              LOGGER.trace(s"Node[$adnIdOpt] missing or not accessable by user[${pw.personId}]")
              SioReqMd.fromPwOpt(pwOpt)
          }
          srmFut flatMap { srm =>
            val req1 = RequestWithNodeOpt(mnodeOpt1, srm, pwOpt, request)
            block(req1)
          }
        }

      case None =>
        IsAuth.onUnauth(request)
    }
  }

}


case class RequestWithNodeOpt[A](
  mnodeOpt : Option[MAdnNode],
  sioReqMd : SioReqMd,
  pwOpt    : PwOpt_t,
  request  : Request[A]
) extends AbstractRequestWithPwOpt(request)


sealed trait IsAdnNodeAdminOptOrAuthBase2 extends IsAdnNodeAdminOptOrAuthBase with ExpireSession[RequestWithNodeOpt]

case class IsAdnNodeAdminOptOrAuth(adnIdOpt: Option[String])
  extends IsAdnNodeAdminOptOrAuthBase2
