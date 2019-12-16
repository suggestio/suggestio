package util.acl

import io.suggest.es.model.EsModel
import javax.inject.Inject
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import models.req.{MPersonReq, MReq}
import play.api.mvc._
import io.suggest.req.ReqUtil
import models.mproj.ICommonDi
import play.api.http.Status

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 22:33
 * Description: Гибрид IsSuperuser и чтения произвольного юзера из хранилища по id.
 */

class IsSuPerson @Inject()(
                            esModel   : EsModel,
                            mNodes    : MNodes,
                            aclUtil   : AclUtil,
                            isSu      : IsSu,
                            reqUtil   : ReqUtil,
                            mCommonDi : ICommonDi
                          ) {

  import mCommonDi._
  import esModel.api._

  /** @param personId id юзера. */
  def apply(personId: String): ActionBuilder[MPersonReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MPersonReq] {

      override def invokeBlock[A](request: Request[A], block: (MPersonReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (user.isSuper) {

          // Если юзер запрашивает сам себя, то заполняем user.personNodeOptFut. Иначе запрашиваем узел целевого юзера напрямую.
          val mpersonOptFut: Future[Option[MNode]] = {
            if (user.personIdOpt contains personId) {
              user.personNodeOptFut
            } else {
              mNodes
                .getByIdCache(personId)
                .withNodeType(MNodeTypes.Person)
            }
          }

          mpersonOptFut.flatMap {
            _.fold {
              personNotFound(request)
            } { mperson =>
              val req1 = MPersonReq(mperson, request, user)
              block(req1)
            }
          }

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

      /** Юзер не найден. */
      def personNotFound(request: Request[_]): Future[Result] =
        errorHandler.onClientError(request, Status.NOT_FOUND, s"person not exists: $personId")

    }
  }

}
