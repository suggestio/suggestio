package util.acl

import javax.inject.Singleton

import com.google.inject.Inject
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import models.req.MReqHdr
import play.api.mvc._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.10.17 18:37
  * Description: Проверка возможности открывать унифицированный websocket-канал.
  * Канал может открывать любой юзер, имеющий корректный ctx-id на руках.
  */
@Singleton
class CanOpenWsChannel @Inject()(
                                  aclUtil                    : AclUtil,
                                  mCtxIds                    : MCtxIds
                                )
  extends MacroLogsImplLazy
{

  /** Непосредственный код проверки */
  def can(ctxId: MCtxId)(implicit rh: RequestHeader): Option[MReqHdr] = {
    val user = aclUtil.userFromRequest(rh)

    if (mCtxIds.validate(ctxId, user.personIdOpt)) {
      val mreq = MReqHdr(rh, user)
      Some(mreq)
    } else {
      LOGGER.warn(s"Invalid ctxId for user#${user.personIdOpt.orNull}, userMatchesCtxId?${user.personIdOpt ==* ctxId.personId}, raw ctxId = $ctxId")
      None
    }
  }


  // 2017-10-10 Код ниже закомменчен, потому что websocket'ы несовместимы с этим со всем.
  // Возможно, это потом понадобится, или TODO удалить окончательно

  /** Общий код для Action и ActionBuilder, проверяющих доступ на открытие унифицированного ws-канала. */
  /*
  private def _apply[A](ctxId: MCtxId, request0: Request[A])(f: MReq[A] => Future[Result]): Future[Result] = {
    can(ctxId, request0).fold [Future[Result]] {
      Results.Forbidden("CtxId is not valid.")
    } { mReqHdr =>
      val mreq = MReq(request0, mReqHdr.user)
      f(mreq)
    }
  }
  */



  /** Сборка ActionBuilder'а, проверяющего возможность для аплоада файла. */
  /*
  def apply(ctxId: MCtxId): ActionBuilder[MReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MReq] {
      override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
        _apply(ctxId, request)(block)
      }
    }
  }
  */


  /** Сборка заворачивающего экшена, который проверяет возможность для аплоада файла. */
  /*
  def A[A](ctxId: MCtxId)(action: Action[A]): Action[A] = {
    dab.async(action.parser) { request =>
      _apply(ctxId, request)(action.apply)
    }
  }
  */

}
