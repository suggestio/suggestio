package util.acl

import com.google.inject.Inject
import models.mcal.MCalendars
import models.mproj.ICommonDi
import models.req.MCalendarReq
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.www.util.acl.SioActionBuilderOuter

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:48
 * Description: Доступ к календарю вообще без проверки ACL.
 */
class CalendarAccessAny @Inject() (
                                    mCalendars  : MCalendars,
                                    mCommonDi   : ICommonDi
                                  )
  extends SioActionBuilderOuter
{

  import mCommonDi._


  /** @param calId id календаря, с которым происходит взаимодействие. */
  def apply(calId: String): ActionBuilder[MCalendarReq] = {
    new SioActionBuilderImpl[MCalendarReq] {

      def calNotFound(request: Request[_]): Future[Result] = {
        Results.NotFound(s"Calendar $calId does not exist.")
      }

      override def invokeBlock[A](request: Request[A], block: (MCalendarReq[A]) => Future[Result]): Future[Result] = {
        val mcalOptFut = mCalendars.getById(calId)
        val personIdOpt = sessionUtil.getPersonId(request)
        val user = mSioUsers(personIdOpt)

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
