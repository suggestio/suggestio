package util.acl

import io.suggest.es.model.EsModel
import javax.inject.Inject
import models.mcal.MCalendars
import models.req.{MCalendarReq, MReq}
import play.api.mvc._
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 17:30
 * Description: Поддержка контроля доступа к календарям для суперюзеров.
 * Долгое время это счастье жило прямо в контроллере.
 */
final class IsSuCalendar @Inject()(
                                    injector: Injector,
                                  ) {

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val mCalendars = injector.instanceOf[MCalendars]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /**
    * @param calId id календаря, вокруг которого идёт работа.
    */
  def apply(calId: String): ActionBuilder[MCalendarReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MCalendarReq] {

      override def invokeBlock[A](request: Request[A], block: (MCalendarReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (user.isSuper) {
          import esModel.api._

          mCalendars.getById(calId).flatMap {
            case Some(mcal) =>
              val req1 = MCalendarReq(mcal, request, user)
              block(req1)

            case None =>
              httpErrorHandler.onClientError( request, Status.NOT_FOUND, s"Calendar not found: $calId")
          }

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

    }
  }

}
