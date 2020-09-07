package util.acl

import javax.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.util.logs.MacroLogsImpl
import models.req._
import play.api.mvc._
import util.n2u.N2NodesUtil
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Проверка прав на управление рекламной карточкой.
 */

/** Аддон для контроллеров, занимающихся редактированием рекламных карточек. */
final class CanEditAd @Inject() (
                                  injector                : Injector,
                                )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private val ec = injector.instanceOf[ExecutionContext]


  /** Кое какая утиль для action builder'ов, редактирующих карточку. */
  trait AdEditBase {
    /** id рекламной карточки, которую клиент хочет поредактировать. */
    def adId: String

    def forbiddenFut[A](msg: String, request: Request[A]): Future[Result] =
      errorHandler.onClientError( request, Status.FORBIDDEN, s"No access to ad#$adId: $msg" )

    def adNotFound(req: IReqHdr): Future[Result] = {
      LOGGER.trace(s"invokeBlock(): Ad not found: $adId")
      errorHandler.onClientError(req, Status.NOT_FOUND)
    }

  }

  sealed case class MAdProd(mad: MNode, producer: MNode)

  def isUserCanEditAd(user: ISioUser, adId: String): Future[Option[MAdProd]] = {
    FutureUtil.optFut2futOpt(user.personIdOpt) { _ =>
      import esModel.api._

      val madOptFut = mNodes
        .getByIdCache(adId)
        .withNodeType(MNodeTypes.Ad)

      lazy val logPrefix = s"isUserCanEditAd1(${user.personIdOpt.orNull}, $adId):"
      madOptFut.flatMap { madOpt =>
        if (madOpt.isEmpty)
          LOGGER.debug(s"$logPrefix Ad not found.")
        FutureUtil.optFut2futOpt(madOpt) { mad =>
          isUserCanEditAd(user, mad)
        }
      }
    }
  }
  def isUserCanEditAd(user: ISioUser, mad: MNode): Future[Option[MAdProd]] = {
    FutureUtil.optFut2futOpt(user.personIdOpt) { _ =>
      import esModel.api._

      val prodIdOpt = n2NodesUtil.madProducerId(mad)
      val prodNodeOptFut = mNodes.maybeGetByIdCached(prodIdOpt)
      lazy val logPrefix = s"isUserCanEditAd2(${user.personIdOpt.orNull}, ${mad.id.orNull}):"
      for (prodNodeOpt <- prodNodeOptFut) yield {
        if (prodNodeOpt.isEmpty)
          LOGGER.warn(s"$logPrefix Missing producer ${prodIdOpt.orNull}")

        for {
          producer <- prodNodeOpt
          if isUserCanEditAd(user, mad = mad, producer = producer)
        } yield {
          MAdProd(mad = mad, producer = producer)
        }
      }
    }
  }
  def isUserCanEditAd(user: ISioUser, mad: MNode, producer: MNode): Boolean = {
    // isSuper не проверяем, потому что оно и так проверяется внутри isNodeAdminCheck().
    val isProducerAdmin = user.isAuth && isNodeAdmin.isNodeAdminCheck(producer, user)
    LOGGER.trace(s"isUserCanEditAd3(${user.personIdOpt.orNull}, ad#${mad.id.orNull}, prod#${producer.id.orNull}): producerId = ${producer.id.orNull}, isAdmin?$isProducerAdmin")
    isProducerAdmin
  }


  def apply(adId1: String, userInits1: MUserInit*): ActionBuilder[MAdProdReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MAdProdReq] with AdEditBase {

      override def adId = adId1

      override def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        user.personIdOpt.fold (isAuth.onUnauth(request)) { _ =>
          val producerNodeIfCanEditOptFut = isUserCanEditAd(user, adId)

          MUserInits.initUser(user, userInits1)

          producerNodeIfCanEditOptFut.flatMap {
            case Some(data) =>
              val req1 = MAdProdReq(mad = data.mad, producer = data.producer, request = request, user = user)
              block(req1)
            case None =>
              LOGGER.debug(s"isEditAllowed($adId, $user): Not a producer admin or missing nodes.")
              forbiddenFut("No node admin rights", request)
          }
        }
      }

    }
  }

}


trait ICanEditAdDi {
  def canEditAd: CanEditAd
}

