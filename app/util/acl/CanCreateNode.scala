package util.acl

import io.suggest.ym.model.MAdnNode
import play.api.mvc.{Results, Result, Request, ActionBuilder}
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.PlayMacroLogsDyn
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.02.15 10:16
 * Description: Создание новой ноды юзером возможно, если юзер залогинен, и если не превышен
 * лимит кол-ва имеющихся узлов.
 */
object CanCreateNodeUtil {

  /** Макс.кол-во узлов во владении юзера, после которого юзер уже не может создавать узлы. */
  val USER_NODES_MAX = configuration.getInt("adn.nodes.user.create.max") getOrElse 10

}


import CanCreateNodeUtil._


/** Абстрактное действо по проверки прав на создание ноды. */
trait CanCreateNodeBase extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsDyn {
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) =>
        // Юзер залогинен. Нужно посчитать, сколько у него уже есть узлов.
        val pcntFut = MAdnNode.countByPersonId(pw.personId)
          .map { _.toInt }
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        pcntFut flatMap { pcnt =>
          val max = USER_NODES_MAX
          if (pcnt >= max) {
            LOGGER.warn(s"User[${pw.personId}] tried to create new node, but limit[$max] exceeded: user have $pcnt nodes.")
            tooManyNodesFut(request, pwOpt, pcnt)
          } else {
            LOGGER.trace(s"User [${pw.personId}] allowed to create new node. User already have $pcnt nodes.")
            srmFut flatMap { srm =>
              val req1 = RequestWithPwOpt(pwOpt, request, srm)
              block(req1)
            }
          }
        }

      case None =>
        LOGGER.debug("Refused for anonymous. Session expired?")
        IsAuth.onUnauth(request)
    }
  }

  /** Что возвращать юзеру, когда слишком много нод. */
  def tooManyNodesFut(request: Request[_], pwOpt: PwOpt_t, nodesCnt: Int): Future[Result] = {
    val res = Results.Forbidden("Too many nodes: " + nodesCnt)
    Future successful res
  }
}


sealed trait CanCreateNodeBase2 extends CanCreateNodeBase with ExpireSession[AbstractRequestWithPwOpt]

/** Реализация [[CanCreateNodeBase]] без CSRF-действий. */
object CanCreateNode      extends CanCreateNodeBase2

/** Реализация [[CanCreateNodeBase]] с выставлением CSRF-токена в сессию. */
object CanCreateNodeGet   extends CanCreateNodeBase2 with CsrfGet[AbstractRequestWithPwOpt]

/** Реализация [[CanCreateNodeBase]] с проверкой CSRF-токена. */
object CanCreateNodePost  extends CanCreateNodeBase2 with CsrfPost[AbstractRequestWithPwOpt]
