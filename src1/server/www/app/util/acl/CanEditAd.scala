package util.acl

import javax.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.req.{IReqHdr, MAdProdReq, MReq, MUserInit}
import play.api.mvc._
import util.n2u.N2NodesUtil
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.req.ReqUtil
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
          val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)

          maybeInitUser(user)
          def mreq = MReq(request, user)

          madOptFut.flatMap {
            case Some(mad) =>
              val prodIdOpt = n2NodesUtil.madProducerId(mad)
              val prodNodeOptFut = mNodesCache.maybeGetByIdCached( prodIdOpt )

              prodNodeOptFut.flatMap {
                case Some(producer) =>
                  val allowed = user.isSuper || isNodeAdmin.isNodeAdminCheck(producer, user)

                  if (!allowed) {
                    LOGGER.debug(s"isEditAllowed(${mad.id.get}, $user): Not a producer[$prodIdOpt] admin.")
                    forbiddenFut("No node admin rights", request)
                  } else {
                    val req1 = MAdProdReq(mad = mad, producer = producer, request = request, user = user)
                    block(req1)
                  }

                case None =>
                  prodNotFound( mreq, prodIdOpt )
              }

            case None =>
              adNotFound(mreq)
          }
        }
      }

    }
  }

}
