package util.acl

import controllers.SioController
import models.ai.IMAiMads
import models.req.{MAiMadReq, MReq}

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 14:24
 * Description: Аддон для контроллеров для IsSuperuser + доступ к AiMad по id.
 */
trait IsSuperuserAiMad
  extends SioController
  with Csrf
  with IMAiMads
{

  import mCommonDi._

  /** IsSuperuser + доступ к указанному MAiMad. */
  trait IsSuAiMadBase
    extends ActionBuilder[MAiMadReq]
    with IsSuperuserUtil
  {

    /** id описания карточки. */
    def aiMadId: String

    override def invokeBlock[A](request: Request[A], block: (MAiMadReq[A]) => Future[Result]): Future[Result] = {
      val madAiFut = mAiMads.getById(aiMadId)
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      if (user.isSuper) {
        madAiFut.flatMap {
          case Some(madAi) =>
            val req1 = MAiMadReq(madAi, request, user)
            block(req1)

          case None => aiMadNotFound
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    def aiMadNotFound: Future[Result] = {
      NotFound(s"MAiMad($aiMadId) not found.")
    }

  }


  abstract class IsSuAiMadAbstract
    extends IsSuAiMadBase
    with ExpireSession[MAiMadReq]


  case class IsSuAiMad(override val aiMadId: String)
    extends IsSuAiMadAbstract

  case class IsSuAiMadGet(override val aiMadId: String)
    extends IsSuAiMadAbstract
    with CsrfGet[MAiMadReq]

  case class IsSuAiMadPost(override val aiMadId: String)
    extends IsSuAiMadAbstract
    with CsrfPost[MAiMadReq]

}
