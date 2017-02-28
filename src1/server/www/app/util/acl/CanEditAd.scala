package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.req.{IReqHdr, MAdProdReq, MReq, MUserInit}
import play.api.mvc._
import util.n2u.N2NodesUtil
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter
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
                            isAdnNodeAdmin          : IsAdnNodeAdmin,
                            n2NodesUtil             : N2NodesUtil,
                            isAuth                  : IsAuth,
                            mCommonDi               : ICommonDi
                          )
  extends SioActionBuilderOuter
  with MacroLogsImpl
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


  def apply(adId1: String, userInits1: MUserInit*): ActionBuilder[MAdProdReq] = {
    new SioActionBuilderImpl[MAdProdReq] with AdEditBase with InitUserCmds {

      override def adId = adId1

      override def userInits = userInits1

      def prodNotFound(nodeIdOpt: Option[String]): Future[Result] = {
        Results.NotFound("Ad producer not found: " + nodeIdOpt)
      }

      override def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
        val personIdOpt = sessionUtil.getPersonId(request)

        personIdOpt.fold (isAuth.onUnauth(request)) { personId =>
          val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)
          val user = mSioUsers(personIdOpt)

          maybeInitUser(user)

          madOptFut.flatMap {
            case Some(mad) =>
              val prodIdOpt = n2NodesUtil.madProducerId(mad)
              val prodNodeOptFut = mNodesCache.maybeGetByIdCached( prodIdOpt )

              prodNodeOptFut.flatMap {
                case Some(producer) =>
                  val allowed = user.isSuper || isAdnNodeAdmin.isAdnNodeAdminCheck(producer, user)

                  if (!allowed) {
                    LOGGER.debug(s"isEditAllowed(${mad.id.get}, $user): Not a producer[$prodIdOpt] admin.")
                    forbiddenFut("No node admin rights", request)
                  } else {
                    val req1 = MAdProdReq(mad = mad, producer = producer, request = request, user = user)
                    block(req1)
                  }

                case None =>
                  prodNotFound(personIdOpt)
              }

            case None =>
              val req1 = MReq(request, user)
              adNotFound(req1)
          }
        }
      }

    }
  }

}
