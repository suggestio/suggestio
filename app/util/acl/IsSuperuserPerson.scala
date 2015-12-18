package util.acl

import controllers.SioController
import io.suggest.model.n2.node.MNodeTypes
import models.MNode
import models.req.{MPersonReq, SioReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 22:33
 * Description: Гибрид IsSuperuser и чтения произвольного юзера из хранилища по id.
 */

trait IsSuperuserPerson
  extends SioController
  with IsSuperuserUtilCtl
{

  import mCommonDi._

  trait IsSuperuserPersonBase
    extends ActionBuilder[MPersonReq]
    with IsSuperuserUtil
  {
    /** id юзера. */
    def personId: String

    override def invokeBlock[A](request: Request[A], block: (MPersonReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuperUser) {

        // Если юзер запрашивает сам себя, то заполняем user.personNodeOptFut. Иначе запрашиваем узел целевого юзера напрямую.
        val mpersonOptFut: Future[Option[MNode]] = {
          val _personId = personId
          if (personIdOpt.contains(_personId)) {
            user.personNodeOptFut
          } else {
            mNodeCache.getByIdType(_personId, MNodeTypes.Person)
          }
        }

        mpersonOptFut.flatMap {
          case Some(mperson) =>
            val req1 = MPersonReq(mperson, request, user)
            block(req1)
          case None =>
            personNotFound(request)
        }

      } else {
        val req1 = SioReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    /** Юзер не найден. */
    def personNotFound(request: Request[_]): Future[Result] = {
      NotFound("person not exists: " + personId)
    }

  }


  /** Дефолтовая реализация [[IsSuperuserPersonBase]]. */
  case class IsSuperuserPerson(personId: String)
    extends IsSuperuserPersonBase
    with ExpireSession[MPersonReq]

}
