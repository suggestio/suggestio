package util.acl

import javax.inject.Singleton

import com.google.inject.Inject
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.util.logs.MacroLogsImplLazy
import models.req.{MReqHdr, MSioUsers}
import play.api.mvc._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.10.17 18:37
  * Description: Проверка возможности открывать унифицированный websocket-канал.
  *
  * Канал может открывать любой клиент, имеющий корректный ctx-id на руках.
  *
  * СЕССИЯ ИГНОРИРУЕТСЯ, потому что кукис может быть не передан на ту ноду, до которой открыт канал.
  */
@Singleton
class CanOpenWsChannel @Inject()(
                                  mSioUsers                  : MSioUsers,
                                  mCtxIds                    : MCtxIds
                                )
  extends MacroLogsImplLazy
{

  /** Непосредственный код проверки */
  def can(ctxId: MCtxId)(implicit rh: RequestHeader): Option[MReqHdr] = {
    if (mCtxIds.checkSig(ctxId)) {
      val user = mSioUsers( ctxId.personId )
      val mreq = MReqHdr(rh, user)
      Some(mreq)
    } else {
      LOGGER.warn(s"Invalid ctxId for personIdFromURL#${ctxId.personId.orNull}, raw ctxId = $ctxId, session ignored")
      None
    }
  }

}
