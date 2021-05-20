package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNodeTypes, MNodes}

import javax.inject.Inject
import models.mproj.ICommonDi
import models.req.MNodeReq
import play.api.mvc._
import io.suggest.req.ReqUtil
import play.api.http.Status

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:48
 * Description: Доступ к календарю вообще без проверки ACL.
 */
final class CalendarAccessAny @Inject() (
                                          esModel               : EsModel,
                                          aclUtil               : AclUtil,
                                          mNodes                : MNodes,
                                          reqUtil               : ReqUtil,
                                          mCommonDi             : ICommonDi
                                        )
{

  import mCommonDi.{ec, errorHandler}
  import esModel.api._


  /** @param calId id календаря, с которым происходит взаимодействие. */
  def apply(calId: String): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      def calNotFound(request: Request[_]): Future[Result] =
        errorHandler.onClientError( request, Status.NOT_FOUND, s"Calendar $calId does not exist.")

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val mcalOptFut = mNodes
          .getById( calId )
          .withNodeType( MNodeTypes.Calendar )
        val user = aclUtil.userFromRequest(request)

        mcalOptFut.flatMap {
          case Some(mcal) =>
            val req1 = MNodeReq(mcal, request, user)
            block(req1)

          case None =>
            calNotFound(request)
        }
      }

    }
  }

}
