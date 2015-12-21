package util.acl

import controllers.SioController
import models.req.{MReq, MNodeEactReq}
import models.usr.{EmailPwIdent, EmailActivation}
import play.api.mvc.{Result, ActionBuilder, Request}
import util.PlayMacroLogsI
import views.html.lk.adn.invite.inviteInvalidTpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.15 11:35
 * Description: ActionBuilder'ы для доступа к инвайтам на управление узлом через email.
 * Аддон подмешивается к контроллерам, где необходима поддержка NodeEact.
 */
trait NodeEact
  extends SioController
  with PlayMacroLogsI
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Абстрактная логика ActionBuider'ов, обрабатывающих запросы активации инвайта на узел. */
  sealed trait NodeEactBase
    extends ActionBuilder[MNodeEactReq]
    with OnUnauthUtil
  {

    /** id в модели EmailActivation. */
    def eaId: String

    /** id узла, на который клиент пытается получить привелегии. */
    def nodeId: String

    /** Запуск логики экшена. */
    override def invokeBlock[A](request: Request[A], block: (MNodeEactReq[A]) => Future[Result]): Future[Result] = {
      val eaOptFut = EmailActivation.getById(eaId)
      val nodeOptFut = mNodeCache.getById(nodeId)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      /** Общий код рендера отрицательного ответа на запрос вынесен сюда. */
      def _renderInvalidTpl(reason: String): Future[Result] = {
        implicit val req = MReq(request, user)
        NotFound( inviteInvalidTpl(reason) )
      }

      eaOptFut.flatMap {
        case Some(ea) if ea.key == nodeId =>
          val epwIdOptFut = EmailPwIdent.getByEmail(ea.email)
          nodeOptFut flatMap {

            case Some(mnode) =>
              epwIdOptFut.flatMap {
                // email, на который выслан запрос, уже зареган в системе, но текущий юзер не подходит: тут у нас анонимус или левый юзер.
                case Some(epwIdent) if epwIdent.isVerified && !personIdOpt.contains(epwIdent.personId) =>
                  LOGGER.debug(s"eAct has email = ${epwIdent.email}. This is personId[${epwIdent.personId}], but current pwOpt = $personIdOpt :: Rdr user to login...")
                  val result = onUnauthBase(request)
                  val res2 = if (user.isAuth) result.withNewSession else result
                  Future successful res2

                // Юзер анонимус и такие email неизвестны системе, либо тут у нас текущий необходимый юзер.
                case epwIdOpt =>
                  val req1 = MNodeEactReq(mnode, ea, epwIdOpt, request, user)
                  block(req1)
              }

            case None =>
              // should never occur
              LOGGER.error(s"nodeOwnerInviteAcceptGo($nodeId, eaId=$eaId): ADN node not found, but act.code for node exist. This should never occur.")
              _renderInvalidTpl("adn.node.not.found")
          }

        case other =>
          // Неверный код активации или id магазина. Если None, то код скорее всего истёк. Либо кто-то брутфорсит.
          LOGGER.debug(s"nodeOwnerInviteAcceptCommon($nodeId, eaId=$eaId): Invalid activation code (eaId): code not found. Expired?")
          // TODO Надо проверить, есть ли у юзера права на узел, и если есть, то значит юзер дважды засабмиттил форму, и надо его сразу отредиректить в его магазин.
          // TODO Может и быть ситуация, что юзер всё ещё не залогинен, а второй сабмит уже тут. Нужно это тоже как-то обнаруживать. Например через временную сессионную куку из формы.
          LOGGER.warn(s"TODO I need to handle already activated requests!!!")
          _renderInvalidTpl("mart.activation.expired.or.invalid.code")
      }
    }

  }

  sealed abstract class NodeEactAbstract
    extends NodeEactBase
    with ExpireSession[MNodeEactReq]

  /** Реализация NodeEactBase для CSRF+GET-запросов. */
  case class NodeEactGet(override val nodeId: String, override val eaId: String)
    extends NodeEactAbstract
    with CsrfGet[MNodeEactReq]

  /** Реализация NodeEactBase для CSRF+POST-запросов. */
  case class NodeEactPost(override val nodeId: String, override val eaId: String)
    extends NodeEactAbstract
    with CsrfPost[MNodeEactReq]

}
