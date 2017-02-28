package util.acl

import com.google.inject.Inject
import models.ai.MAiMads
import models.mproj.ICommonDi
import models.req.{MAiMadReq, MReq}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter

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
                            isSu                    : IsSu,
                            mCommonDi               : ICommonDi
                          )
  extends SioActionBuilderOuter
{

  import mCommonDi._

  /** IsSuperuser + доступ к указанному MAiMad.
    *
    * @param aiMadId id описания карточки.
    */
  def apply(aiMadId: String): ActionBuilder[MAiMadReq] = {
    new SioActionBuilderImpl[MAiMadReq] {

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
          isSu.supOnUnauthFut(req1)
        }
      }

      def aiMadNotFound: Future[Result] = {
        Results.NotFound(s"MAiMad($aiMadId) not found.")
      }

    }
  }

}
