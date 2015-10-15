package util.acl

import controllers.SioController
import io.suggest.di.{IExecutionContext, IEsClient}
import models.MAdnNodeCache
import models.req.SioReqMd
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:48
 * Description: Аддон для контроллеров с гибридом IsSuperuser + MAdnNode.getById().
 */
trait IsSuperuserAdnNode
  extends SioController
  with IEsClient
  with IExecutionContext
  with IsSuperuserUtilCtl
{

  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
  sealed trait IsSuperuserAdnNodeBase
    extends ActionBuilder[RequestForAdnNodeAdm]
    with IsSuperuserUtil
  {

    /** id запрашиваемого узла. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (RequestForAdnNodeAdm[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper.isSuperuser(pwOpt)) {
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        MAdnNodeCache.getById(adnId) flatMap {
          case Some(adnNode) =>
            sioReqMdFut flatMap { srm =>
              block(RequestForAdnNodeAdm(adnNode, isMyNode = true, request, pwOpt, srm))
            }
          case None =>
            nodeNotFound
        }

      } else {
        onUnauthFut(request, pwOpt)
      }
    }

    def nodeNotFound: Future[Result] = {
      val render = NotFound("Adn node not found: " + adnId)
      Future successful render
    }

  }

  sealed abstract class IsSuperuserAdnNodeBase2
    extends IsSuperuserAdnNodeBase
    with ExpireSession[RequestForAdnNodeAdm]

  /**
   * Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
   * @param adnId id рекламного узла, которым интересуется суперпользователь.
   */
  case class IsSuperuserAdnNode(override val adnId: String)
    extends IsSuperuserAdnNodeBase2

  /** Аналог [[IsSuperuserAdnNode]] с выставлением CSRF-токена. */
  case class IsSuperuserAdnNodeGet(override val adnId: String)
    extends IsSuperuserAdnNodeBase2
    with CsrfGet[RequestForAdnNodeAdm]

  /** Аналог [[IsSuperuserAdnNode]] с проверкой CSRF-токена при сабмитах. */
  case class IsSuperuserAdnNodePost(override val adnId: String)
    extends IsSuperuserAdnNodeBase2
    with CsrfPost[RequestForAdnNodeAdm]

}
