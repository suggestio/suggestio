package util.acl

import javax.inject.Inject
import models.mcal.MCalendars
import models.mproj.ICommonDi
import models.req.MCalendarReq
import play.api.mvc._
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.www.util.req.ReqUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:48
 * Description: Доступ к календарю вообще без проверки ACL.
 */
class CalendarAccessAny @Inject() (
                                    aclUtil               : AclUtil,
                                    mCalendars            : MCalendars,
                                    reqUtil               : ReqUtil,
                                    mCommonDi             : ICommonDi
                                  )
{

  import mCommonDi._


  /** @param calId id календаря, с которым происходит взаимодействие. */
  def apply(calId: String): ActionBuilder[MCalendarReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MCalendarReq] {

      def calNotFound(request: Request[_]): Future[Result] = {
        Results.NotFound(s"Calendar $calId does not exist.")
      }

      override def invokeBlock[A](request: Request[A], block: (MCalendarReq[A]) => Future[Result]): Future[Result] = {
        val mcalOptFut = mCalendars.getById(calId)
        val user = aclUtil.userFromRequest(request)

        mcalOptFut.flatMap {
          case Some(mcal) =>
            val req1 = MCalendarReq(mcal, request, user)
            block(req1)

          case None =>
            calNotFound(request)
        }
      }

    }

  }

}
