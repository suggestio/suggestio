package util.acl

import controllers.SioController
import io.suggest.di.{IEsClient, IExecutionContext}
import models.ai.MAiMad
import models.req.SioReqMd
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 14:24
 * Description: Аддон для контроллеров для IsSuperuser + доступ к AiMad по id.
 */
trait IsSuperuserAiMad
  extends SioController
  with IExecutionContext
  with IEsClient
  with IsSuperuserUtilCtl
{

  /** IsSuperuser + доступ к указанному MAiMad. */
  trait IsSuperuserAiMadBase
    extends ActionBuilder[AiMadRequest]
    with IsSuperuserUtil
  {

    /** id */
    def aiMadId: String

    override def invokeBlock[A](request: Request[A], block: (AiMadRequest[A]) => Future[Result]): Future[Result] = {
      val madAiFut = MAiMad.getById(aiMadId)
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper isSuperuser pwOpt) {
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        madAiFut flatMap {
          case Some(madAi) =>
            srmFut flatMap { srm =>
              val req1 = AiMadRequest(madAi, pwOpt, request, srm)
              block(req1)
            }

          case None => aiMadNotFound
        }
      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

    def aiMadNotFound: Future[Result] = {
      val res = NotFound(s"MAiMad($aiMadId) not found.")
      Future successful res
    }
  }


  case class IsSuperuserAiMad(override val aiMadId: String)
    extends IsSuperuserAiMadBase
    with ExpireSession[AiMadRequest]

}


/** Реквест с доступом к aiMad. */
case class AiMadRequest[A](
  aiMad     : MAiMad,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)

