package util.acl

import controllers.SioController
import models.MNode
import models.req.SioReqMd
import models.usr.{EmailPwIdent, EmailActivation}
import play.api.mvc.{Result, ActionBuilder, Request}
import util.PlayMacroLogsI
import util.acl.PersonWrapper.PwOpt_t
import views.html.lk.adn.invite.inviteInvalidTpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.15 11:35
 * Description: ActionBuilder'ы для доступа к инвайтам на управление узлом через email.
 * Аддон подмешивается к контроллерам, где необходима поддержка NodeEact.
 */
trait NodeEactAcl
  extends SioController
  with PlayMacroLogsI
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Абстрактная логика ActionBuider'ов, обрабатывающих запросы активации инвайта на узел. */
  sealed trait NodeEactBase
    extends ActionBuilder[NodeEactRequest]
    with OnUnauthUtil
  {

    /** id в модели EmailActivation. */
    def eaId: String

    /** id узла, на который клиент пытается получить привелегии. */
    def nodeId: String

    /** Запуск логики экшена. */
    override def invokeBlock[A](request: Request[A], block: (NodeEactRequest[A]) => Future[Result]): Future[Result] = {
      val eaOptFut = EmailActivation.getById(eaId)
      val nodeOptFut = mNodeCache.getById(nodeId)

      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)

      /** Общий код рендера отрицательного ответа на запрос вынесен сюда. */
      def _renderInvalidTpl(reason: String): Future[Result] = {
        srmFut map { srm =>
          implicit val req = RequestWithPwOpt(pwOpt, request, srm)
          NotFound( inviteInvalidTpl(reason) )
        }
      }

      eaOptFut.flatMap {
        case Some(ea) if ea.key == nodeId =>
          val epwIdOptFut = EmailPwIdent.getByEmail(ea.email)
          nodeOptFut flatMap {

            case Some(mnode) =>
              epwIdOptFut flatMap {
                // email, на который выслан запрос, уже зареган в системе, но текущий юзер не подходит: тут у нас анонимус или левый юзер.
                case Some(epwIdent) if epwIdent.isVerified && !pwOpt.exists(_.personId == epwIdent.personId) =>
                  LOGGER.debug(s"eAct has email = ${epwIdent.email}. This is personId[${epwIdent.personId}], but current pwOpt = ${pwOpt.map(_.personId)} :: Rdr user to login...")
                  val result = onUnauthBase(request)
                  val isAuth = pwOpt.isDefined
                  val res2 = if (isAuth) result.withNewSession else result
                  Future successful res2

                // Юзер анонимус и такие email неизвестны системе, либо тут у нас текущий необходимый юзер.
                case epwIdOpt =>
                  srmFut flatMap { srm =>
                    val req1 = NodeEactRequest(mnode, ea, epwIdOpt, pwOpt, request, srm)
                    block(req1)
                  }
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

  /** Реализация NodeEactBase для CSRF+GET-запросов. */
  case class NodeEactGet(override val nodeId: String, override val eaId: String)
    extends NodeEactBase
    with CsrfGet[NodeEactRequest]
    with ExpireSession[NodeEactRequest]

  /** Реализация NodeEactBase для CSRF+POST-запросов. */
  case class NodeEactPost(override val nodeId: String, override val eaId: String)
    extends NodeEactBase
    with CsrfPost[NodeEactRequest]
    with ExpireSession[NodeEactRequest]

}


/** Реквест, передаваемый из реализаций [[NodeEactAcl.NodeEactBase]]. */
case class NodeEactRequest[A](
  mnode     : MNode,
  eact      : EmailActivation,
  epwIdOpt  : Option[EmailPwIdent],
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt[A](request)
