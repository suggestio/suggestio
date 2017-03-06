package util.acl

import com.google.inject.Inject
import models.mproj.ICommonDi
import models.req.{IReq, ISioUser}
import play.api.mvc.RequestHeader

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 12:51
  * Description: Утиль для ACL.
  */
class AclUtil @Inject() (mCommonDi: ICommonDi) {

  import mCommonDi._

  /**
    * Извлечь инстанс user из абстрактного реквеста, если возможно.
    * Если невозможно, то собрать новый инстанс.
    */
  def userFromRequest(request: RequestHeader): ISioUser = {
    request match {
      // Это sio-реквест. Значит инстанс user уже существует.
      case req: IReq[_] =>
        req.user

      // Это не sio-реквест, а play-реквест или какой-то другой инстанс. Поэтому user внутри нема.
      case _ =>
        val personIdOpt = sessionUtil.getPersonId(request)
        mSioUsers(personIdOpt)
    }
  }

}
