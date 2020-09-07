package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import javax.inject.Inject
import io.suggest.req.ReqUtil
import models.req.{MNodeOptReq, MUserInit, MUserInits}
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:37
 * Description: ACL для проверки доступа к узлу (если id передан), но при отсуствии доступа просто нет узла в реквесте.
 * sioReqMd отрабатывается аналогично.
 * Появилось для lkList, где по дизайну было наличие текущей ноды, но для шаблона это было необязательно.
 */

final class IsAdnNodeAdminOptOrAuth @Inject() (
                                                injector                : Injector,
                                              ) {

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Сборка action-builder'ов, занимающихся вышеописанной проверкой.
    * @param nodeIdOpt id узла, если есть.
    */
  def apply(nodeIdOpt: Option[String], userInits1: MUserInit*): ActionBuilder[MNodeOptReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeOptReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeOptReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        user.personIdOpt.fold( isAuth.onUnauth(request) ) { _ =>
          import esModel.api._

          val mnodeOptFut = mNodes.maybeGetByIdCached(nodeIdOpt)

          // Запустить в фоне получение кошелька юзера, т.к. экшены все относятся к этому кошельку
          MUserInits.initUser(user, userInits1)

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
