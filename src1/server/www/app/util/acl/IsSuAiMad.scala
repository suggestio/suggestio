package util.acl

import com.google.inject.Inject
import models.ai.MAiMads
import models.mproj.ICommonDi
import models.req.{MAiMadReq, MReq}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result, Results}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 14:24
 * Description: Аддон для контроллеров для IsSuperuser + доступ к AiMad по id.
 */
class IsSuAiMad @Inject() (
                            mAiMads                 : MAiMads,
                            override val mCommonDi  : ICommonDi
                          )
  extends Csrf
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
      Results.NotFound(s"MAiMad($aiMadId) not found.")
    }

  }


  abstract class IsSuAiMadAbstract
    extends IsSuAiMadBase
    with ExpireSession[MAiMadReq]


  case class IsSuAiMad(override val aiMadId: String)
    extends IsSuAiMadAbstract
  @inline
  def apply(aiMadId: String) = IsSuAiMad(aiMadId)

  case class Get(override val aiMadId: String)
    extends IsSuAiMadAbstract
    with CsrfGet[MAiMadReq]

  case class Post(override val aiMadId: String)
    extends IsSuAiMadAbstract
    with CsrfPost[MAiMadReq]

}
