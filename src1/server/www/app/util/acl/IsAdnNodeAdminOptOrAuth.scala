package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsDyn
import models.mproj.ICommonDi
import models.req.{MNodeOptReq, MUserInit}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:37
 * Description: ACL для проверки доступа к узлу (если id передан), но при отсуствии доступа просто нет узла в реквесте.
 * sioReqMd отрабатывается аналогично.
 * Появилось для lkList, где по дизайну было наличие текущей ноды, но для шаблона это было необязательно.
 */

class IsAdnNodeAdminOptOrAuth @Inject() (
                                          isAdnNodeAdmin          : IsAdnNodeAdmin,
                                          val csrf                : Csrf,
                                          isAuth                  : IsAuth,
                                          mCommonDi               : ICommonDi
                                        ) {

  import mCommonDi._

  /** Абстрактная логика работы action-builder'ов, занимающихся вышеописанной проверкой. */
  sealed trait IsAdnNodeAdminOptOrAuthBase
    extends ActionBuilder[MNodeOptReq]
    with MacroLogsDyn
    with InitUserCmds
  {

    /** id узла, если есть. */
    def adnIdOpt: Option[String]

    override def invokeBlock[A](request: Request[A], block: (MNodeOptReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      personIdOpt.fold (isAuth.onUnauth(request)) { _ =>
        val mnodeOptFut = mNodesCache.maybeGetByIdCached(adnIdOpt)
        val user = mSioUsers(personIdOpt)

        // Запустить в фоне получение кошелька юзера, т.к. экшены все относятся к этому кошельку
        maybeInitUser(user)

        mnodeOptFut.flatMap { mnodeOpt =>
          val mnodeOpt1 = mnodeOpt.filter { mnode =>
            isAdnNodeAdmin.isAdnNodeAdminCheck(mnode, user)
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


  case class Get(
    override val adnIdOpt   : Option[String],
    override val userInits  : MUserInit*
  )
    extends IsAdnNodeAdminOptOrAuthBase2
    with csrf.Get[MNodeOptReq]

}
