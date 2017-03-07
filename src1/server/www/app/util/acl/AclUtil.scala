package util.acl

import com.google.inject.Inject
import io.suggest.sec.util.SessionUtil
import models.req.{IReqHdr, ISioUser, MSioUsers}
import play.api.mvc.RequestHeader

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 12:51
  * Description: Утиль для ACL.
  */
class AclUtil @Inject() (
                          mSioUsers   : MSioUsers,
                          sessionUtil : SessionUtil
                        ) {

  /**
    * Извлечь уже готовый инстанс user из абстрактного реквеста, если возможно.
    * Если невозможно, то собрать новый инстанс.
    * Это позволяет избегать пересборки MSioUser, состоящего внутри из кучи lazy val.
    */
  def userFromRequest(request: RequestHeader): ISioUser = {
    request match {
      // Это sio-реквест. Значит инстанс user уже существует.
      case req: IReqHdr =>
        req.user

      // Это не sio-реквест, а play-реквест или какой-то другой инстанс. Поэтому user внутри нема.
      case _ =>
        val personIdOpt = sessionUtil.getPersonId(request)
        mSioUsers(personIdOpt)
    }
  }

}
