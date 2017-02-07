package util.acl

import com.google.inject.Inject
import io.suggest.model.n2.node.MNodeTypes
import models.MNode
import models.req.{MPersonReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 22:33
 * Description: Гибрид IsSuperuser и чтения произвольного юзера из хранилища по id.
 */

class IsSuPerson @Inject()(override val mCommonDi: ICommonDi)
  extends Csrf
{

  import mCommonDi._

  sealed trait IsSuPersonBase
    extends ActionBuilder[MPersonReq]
    with IsSuUtil
  {

    /** id юзера. */
    def personId: String

    override def invokeBlock[A](request: Request[A], block: (MPersonReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuper) {

        // Если юзер запрашивает сам себя, то заполняем user.personNodeOptFut. Иначе запрашиваем узел целевого юзера напрямую.
        val mpersonOptFut: Future[Option[MNode]] = {
          val _personId = personId
          if (personIdOpt.contains(_personId)) {
            user.personNodeOptFut
          } else {
            mNodesCache.getByIdType(_personId, MNodeTypes.Person)
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
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    /** Юзер не найден. */
    def personNotFound(request: Request[_]): Future[Result] = {
      Results.NotFound("person not exists: " + personId)
    }

  }


  sealed abstract class IsSuPersonAbstract
    extends IsSuPersonBase
    with ExpireSession[MPersonReq]


  case class Get(override val personId: String)
    extends IsSuPersonAbstract
    with CsrfGet[MPersonReq]

  case class Post(override val personId: String)
    extends IsSuPersonAbstract
    with CsrfPost[MPersonReq]

}
