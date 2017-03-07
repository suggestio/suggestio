package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.MReq
import play.api.mvc._

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 12:07
  * Description: Поддержка фильтровки по
  */
class IsSuOrNotProduction @Inject() (
                                      aclUtil   : AclUtil,
                                      isSu      : IsSu,
                                      mCommonDi : ICommonDi
                                    )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{


  private def _apply[A](request: Request[A])(f: MReq[A] => Future[Result]): Future[Result] = {
    val user = aclUtil.userFromRequest(request)

    val mreq = MReq(request, user)
    if (user.isSuper || !mCommonDi.isProd) {
      // Проблем нет, провернуть экшен.
      f(mreq)

    } else {
      // Нельзя исполнять текущий экшен.
      LOGGER.warn(s"filter: User#${user.personIdOpt.orNull} not allowed to action, because is not a SU and app.mode=${mCommonDi.current.mode}. ip=${mreq.remoteAddress}")
      isSu.supOnUnauthFut(mreq)
    }
  }


  def apply[A](action: Action[A]): Action[A] = {
    new Action[A] {
      override def parser = action.parser

      override def apply(request: Request[A]): Future[Result] = {
        _apply(request)(action.apply)
      }
    }
  }


  def apply(): ActionBuilder[MReq] = {
    new SioActionBuilderImpl[MReq] {
      override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]) = {
        _apply(request)(block)
      }
    }
  }

}
