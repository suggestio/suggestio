package util.acl

import javax.inject.{Inject, Singleton}

import io.suggest.common.fut.FutureUtil
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.req._
import play.api.mvc._
import util.n2u.N2NodesUtil
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.req.ReqUtil
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Проверка прав на управление рекламной карточкой.
 */

/** Аддон для контроллеров, занимающихся редактированием рекламных карточек. */
@Singleton
class CanEditAd @Inject() (
                            aclUtil                 : AclUtil,
                            isNodeAdmin             : IsNodeAdmin,
                            n2NodesUtil             : N2NodesUtil,
                            isAuth                  : IsAuth,
                            reqUtil                 : ReqUtil,
                            mCommonDi               : ICommonDi
                          )
  extends MacroLogsImpl
{

  import mCommonDi._


  /** Кое какая утиль для action builder'ов, редактирующих карточку. */
  trait AdEditBase {

    /** id рекламной карточки, которую клиент хочет поредактировать. */
    def adId: String

    def forbidden[A](msg: String, request: Request[A]): Result = {
      Results.Forbidden(s"Forbidden access for ad[$adId]: $msg")
    }

    def forbiddenFut[A](msg: String, request: Request[A]): Future[Result] = {
      forbidden(msg, request)
    }

    def adNotFound(req: IReqHdr): Future[Result] = {
      LOGGER.trace(s"invokeBlock(): Ad not found: $adId")
      errorHandler.http404Fut(req)
    }

  }

  sealed case class MAdProd(mad: MNode, producer: MNode)

  def isUserCanEditAd(user: ISioUser, adId: String): Future[Option[MAdProd]] = {
    FutureUtil.optFut2futOpt(user.personIdOpt) { _ =>
      val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)
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
      val prodIdOpt = n2NodesUtil.madProducerId(mad)
      val prodNodeOptFut = mNodesCache.maybeGetByIdCached(prodIdOpt)
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
    new reqUtil.SioActionBuilderImpl[MAdProdReq] with AdEditBase with InitUserCmds {

      override def adId = adId1

      override def userInits = userInits1

      def prodNotFound( mreq: IReqHdr, nodeIdOpt: Option[String]): Future[Result] = {
        Results.NotFound("Ad producer not found: " + nodeIdOpt)
      }

      override def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        user.personIdOpt.fold (isAuth.onUnauth(request)) { _ =>
          val producerNodeIfCanEditOptFut = isUserCanEditAd(user, adId)

          maybeInitUser(user)

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

