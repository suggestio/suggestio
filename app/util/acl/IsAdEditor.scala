package util.acl

import controllers.SioController
import models._
import models.req.{MUserInit, MReq, IReqHdr, MAdProdReq}
import play.api.mvc._
import util.n2u.IN2NodesUtilDi
import util.{PlayMacroLogsDyn, PlayMacroLogsI}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Проверка прав на управление рекламной карточкой.
 */

trait AdEditBaseCtl
  extends SioController
{

  import mCommonDi._

  /** Кое какая утиль для action builder'ов, редактирующих карточку. */
  trait AdEditBase extends PlayMacroLogsI {
    /** id рекламной карточки, которую клиент хочет поредактировать. */
    def adId: String

    def forbidden[A](msg: String, request: Request[A]): Result = {
      Forbidden(s"Forbidden access for ad[$adId]: $msg")
    }

    def forbiddenFut[A](msg: String, request: Request[A]): Future[Result] = {
      val resp = forbidden(msg, request)
      Future.successful(resp)
    }

    def adNotFound(req: IReqHdr): Future[Result] = {
      LOGGER.trace(s"invokeBlock(): Ad not found: $adId")
      errorHandler.http404Fut(req)
    }
  }

}


/** Аддон для контроллеров, занимающихся редактированием рекламных карточек. */
trait CanEditAd
  extends AdEditBaseCtl
  with OnUnauthUtilCtl
  with IN2NodesUtilDi
  with Csrf
{

  import mCommonDi._

  /** Редактировать карточку может только владелец магазина. */
  trait CanEditAdBase
    extends ActionBuilder[MAdProdReq]
    with AdEditBase
    with OnUnauthUtil
    with InitUserCmds
  {

    def prodNotFound(nodeIdOpt: Option[String]): Future[Result] = {
      NotFound("Ad producer not found: " + nodeIdOpt)
    }

    override def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      personIdOpt.fold (onUnauth(request)) { personId =>
        val madOptFut = mNodeCache.getByIdType(adId, MNodeTypes.Ad)
        val user = mSioUsers(personIdOpt)

        maybeInitUser(user)

        madOptFut.flatMap {
          case Some(mad) =>
            val prodIdOpt = n2NodesUtil.madProducerId(mad)
            val prodNodeOptFut = mNodeCache.maybeGetByIdCached( prodIdOpt )

            prodNodeOptFut.flatMap {
              case Some(producer) =>
                val allowed = user.isSuper || IsAdnNodeAdmin.isAdnNodeAdminCheck(producer, user)

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

  sealed abstract class CanEditAd
    extends CanEditAdBase
    with ExpireSession[MAdProdReq]
    with PlayMacroLogsDyn

  /** Запрос формы редактирования карточки должен сопровождаться выставлением CSRF-токена. */
  case class CanEditAdGet(override val adId: String,
                          override val userInits: MUserInit*)
    extends CanEditAd
    with CsrfGet[MAdProdReq]

  /** Сабмит формы редактирования рекламной карточки должен начинаться с проверки CSRF-токена. */
  case class CanEditAdPost(override val adId: String,
                           override val userInits: MUserInit*)
    extends CanEditAd
    with CsrfPost[MAdProdReq]

}
