package util.acl

import com.google.inject.Inject
import models.mcal.MCalendars
import models.req.{MCalendarReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits._
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:30
 * Description: Поддержка контроля доступа к календарям для суперюзеров.
 * Долгое время это счастье жило прямо в контроллере.
 */
class IsSuCalendar @Inject()(
                              mCalendars              : MCalendars,
                              override val mCommonDi  : ICommonDi
                            )
  extends Csrf
{

  import mCommonDi._

  sealed trait IsSuCalendarBase
    extends ActionBuilder[MCalendarReq]
    with IsSuUtil
  {

    /** id календаря, вокруг которого идёт работа. */
    def calId: String

    override def invokeBlock[A](request: Request[A], block: (MCalendarReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      if (user.isSuper) {
        mCalendars.getById(calId).flatMap {
          case Some(mcal) =>
            val req1 = MCalendarReq(mcal, request, user)
            block(req1)

          case None =>
            Results.NotFound("Calendar not found: " + calId)
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }
  }


  /** Частичная реализация [[IsSuCalendarBase]] с поддержкой [[util.acl.ExpireSession]]. */
  sealed abstract class IsSuCalendarAbstract
    extends IsSuCalendarBase
    with ExpireSession[MCalendarReq]

  case class Get(override val calId: String)
    extends IsSuCalendarAbstract
    with CsrfGet[MCalendarReq]

  case class Post(override val calId: String)
    extends IsSuCalendarAbstract
    with CsrfPost[MCalendarReq]

}
