package util.acl

import models.mproj.IMCommonDi
import models.req.{MUserInit, MNodeOptReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.PlayMacroLogsDyn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:37
 * Description: ACL для проверки доступа к узлу (если id передан), но при отсуствии доступа просто нет узла в реквесте.
 * sioReqMd отрабатывается аналогично.
 * Появилось для lkList, где по дизайну было наличие текущей ноды, но для шаблона это было необязательно.
 */

trait IsAdnNodeAdminOptOrAuth
  extends IMCommonDi
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Абстрактная логика работы action-builder'ов, занимающихся вышеописанной проверкой. */
  trait IsAdnNodeAdminOptOrAuthBase
    extends ActionBuilder[MNodeOptReq]
    with PlayMacroLogsDyn
    with OnUnauthUtil
    with InitUserCmds
  {

    /** id узла, если есть. */
    def adnIdOpt: Option[String]

    override def invokeBlock[A](request: Request[A], block: (MNodeOptReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      personIdOpt.fold (onUnauth(request)) { personId =>
        val mnodeOptFut = mNodeCache.maybeGetByIdCached(adnIdOpt)
        val user = mSioUsers(personIdOpt)

        // Запустить в фоне получение кошелька юзера, т.к. экшены все относятся к этому кошельку
        maybeInitUser(user)

        mnodeOptFut.flatMap { mnodeOpt =>
          val mnodeOpt1 = mnodeOpt.filter { mnode =>
            IsAdnNodeAdmin.isAdnNodeAdminCheck(mnode, user)
          }
          val req1 = MNodeOptReq(mnodeOpt1, request, user)
          block(req1)
        }
      }
    }

  }

  sealed abstract class IsAdnNodeAdminOptOrAuthBase2
    extends IsAdnNodeAdminOptOrAuthBase
    with ExpireSession[MNodeOptReq]


  case class IsAdnNodeAdminOptOrAuthGet(
    override val adnIdOpt   : Option[String],
    override val userInits  : MUserInit*
  )
    extends IsAdnNodeAdminOptOrAuthBase2
    with CsrfGet[MNodeOptReq]

}
