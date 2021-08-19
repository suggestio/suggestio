package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNodeTypes, MNodes}

import javax.inject.Inject
import models.req.MNodeReq
import play.api.mvc._
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:48
 * Description: Доступ к календарю вообще без проверки ACL.
 */
final class CalendarAccessAny @Inject() (
                                          injector              : Injector,
                                          aclUtil               : AclUtil,
                                          reqUtil               : ReqUtil,
                                        ) {

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

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
