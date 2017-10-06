package util.acl

import javax.inject.Inject

import io.suggest.req.ReqUtil
import models.mproj.ICommonDi
import models.req.{MNodeOptReq, MUserInit}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

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
                                          aclUtil                 : AclUtil,
                                          isNodeAdmin             : IsNodeAdmin,
                                          isAuth                  : IsAuth,
                                          reqUtil                 : ReqUtil,
                                          mCommonDi               : ICommonDi
                                        ) {

  import mCommonDi._

  /** Сборка action-builder'ов, занимающихся вышеописанной проверкой.
    * @param nodeIdOpt id узла, если есть.
    */
  def apply(nodeIdOpt: Option[String], userInits1: MUserInit*): ActionBuilder[MNodeOptReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeOptReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MNodeOptReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        user.personIdOpt.fold( isAuth.onUnauth(request) ) { _ =>
          val mnodeOptFut = mNodesCache.maybeGetByIdCached(nodeIdOpt)

          // Запустить в фоне получение кошелька юзера, т.к. экшены все относятся к этому кошельку
          maybeInitUser(user)

          mnodeOptFut.flatMap { mnodeOpt =>
            val mnodeOpt1 = mnodeOpt.filter { mnode =>
              isNodeAdmin.isNodeAdminCheck(mnode, user)
            }
            val req1 = MNodeOptReq(mnodeOpt1, request, user)
            block(req1)
          }
        }
      }

    }
  }

}
