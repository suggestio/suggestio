package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import javax.inject.Inject
import io.suggest.req.ReqUtil
import models.req.{MNodeOptReq, MUserInit, MUserInits}
import play.api.mvc._
import japgolly.univeq._
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
final class MaybeAuthMaybeNode @Inject() (
                                           esModel    : EsModel,
                                           injector   : Injector,
                                           reqUtil    : ReqUtil,
                                           aclUtil    : AclUtil,
                                         ) {

  import esModel.api._

  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Собрать MaybeAuth MaybeNode action-builder. */
  def apply(nodeIdOpt: Option[String], userInits1: MUserInit*): ActionBuilder[MNodeOptReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeOptReq] {

      override def invokeBlock[A](request: Request[A],
                                  block: (MNodeOptReq[A]) => Future[Result]): Future[Result] = {
        // Поиск запрошенного узла.
        val nodeOptFut = mNodes.maybeGetByIdCached( nodeIdOpt )

        // Подготовить базовые данные реквеста.
        val user = aclUtil.userFromRequest(request)

        MUserInits.initUser(user, userInits1)

        // Сразу переходим к исполнению экшена, т.к. больше нечего делать.
        (for {
          nodeOpt <- nodeOptFut
          // Если задан несуществующий узел, то выдать 404.
          if nodeOpt.isEmpty ==* nodeIdOpt.isEmpty
          req1 = MNodeOptReq(nodeOpt, request, user)
          res <- block(req1)
        } yield {
          res
        })
          // Отработать отсылку к несуществующему узлу.
          .recoverWith { case _: NoSuchElementException =>
            httpErrorHandler.onClientError( request, Status.NOT_FOUND, s"Node not found: ${nodeIdOpt.orNull}" )
          }
      }

    }
  }

}
