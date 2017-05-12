package util.acl

import com.google.inject.Inject
import io.suggest.sec.util.SessionUtil
import models.req._
import play.api.mvc.{Request, RequestHeader}

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


  /** Получить инстанс [[models.req.IReq]] из абстрактного реквеста.
    *
    * @param request Абстрактный play-реквест.
    * @tparam A Тип body.
    * @return Какой-то инстанс IReq[A].
    */
  def reqFromRequest[A](request: Request[A]): IReq[A] = {
    request match {
      // Обнаружен какой-то sio-реквест. Его и возвращаем.
      case req: IReq[A] =>
        req

      // Какой-то простой реквест. Создать инстанс IReq с ленивым инстансом user.
      case _ =>
        val request1 = request
        new MReqWrap[A] {
          override def request = request1
          override lazy val user = userFromRequest(request1)
        }
    }
  }


  /** Получить инстанс [[models.req.IReqHdr]] из абстрактного RequestHeader'а.
    *
    * @param requestHeader Инстанс play RequestHeader.
    * @return Текущий или новый инстанс [[models.req.IReqHdr]].
    */
  def reqHdrFromRequestHdr(requestHeader: RequestHeader): IReqHdr = {
    requestHeader match {
      case rh: IReqHdr =>
        rh

      case _ =>
        new MReqHdrWrap {
          override def request    = requestHeader
          override lazy val user  = userFromRequest(requestHeader)
        }
    }
  }

}
