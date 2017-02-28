package util.acl

import com.google.inject.Inject
import models.mcal.MCalendars
import models.mproj.ICommonDi
import models.req.MCalendarReq
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits._

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
{

  import mCommonDi._

  trait CalendarAccessAnyBase extends ActionBuilder[MCalendarReq] {

    /** id календаря, с которым происходит взаимодействие. */
    def calId: String

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

  case class CalendarAccessAny(override val calId: String)
    extends CalendarAccessAnyBase

}
