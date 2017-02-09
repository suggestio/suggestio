package util.acl

import com.google.inject.Inject
import io.suggest.sec.util.ExpireSession
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MNodeEactReq, MReq}
import models.usr.{EmailActivations, EmailPwIdents}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.15 11:35
 * Description: ActionBuilder'ы для доступа к инвайтам на управление узлом через email.
 * Аддон подмешивается к контроллерам, где необходима поддержка NodeEact.
 */
class NodeEact @Inject() (
                           emailPwIdents          : EmailPwIdents,
                           emailActivations       : EmailActivations,
                           isAuth                 : IsAuth,
                           val csrf               : Csrf,
                           mCommonDi              : ICommonDi
                         )
  extends MacroLogsImpl
{

  import mCommonDi._

  /** Абстрактная логика ActionBuider'ов, обрабатывающих запросы активации инвайта на узел. */
  sealed trait NodeEactBase
    extends ActionBuilder[MNodeEactReq]
  {

    /** Что делать при проблемах?
      * $1 - reason
      * $2 - sio-реквест.
      */
    def notFoundF: (String, MReq[_]) => Future[Result]

    /** id в модели EmailActivation. */
    def eaId: String

    /** id узла, на который клиент пытается получить привелегии. */
    def nodeId: String

    /** Запуск логики экшена. */
    override def invokeBlock[A](request: Request[A], block: (MNodeEactReq[A]) => Future[Result]): Future[Result] = {
      val eaOptFut = emailActivations.getById(eaId)
      val nodeOptFut = mNodesCache.getById(nodeId)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      /** Общий код рендера отрицательного ответа на запрос вынесен сюда. */
      def _renderInvalidTpl(reason: String): Future[Result] = {
        implicit val req = MReq(request, user)
        notFoundF(reason, req)
      }

      eaOptFut.flatMap {
        case Some(ea) if ea.key == nodeId =>
          val epwIdOptFut = emailPwIdents.getByEmail(ea.email)
          nodeOptFut.flatMap {

            case Some(mnode) =>
              epwIdOptFut.flatMap {
                // email, на который выслан запрос, уже зареган в системе, но текущий юзер не подходит: тут у нас анонимус или левый юзер.
                case Some(epwIdent) if epwIdent.isVerified && !personIdOpt.contains(epwIdent.personId) =>
                  LOGGER.debug(s"eAct has email = ${epwIdent.email}. This is personId[${epwIdent.personId}], but current pwOpt = $personIdOpt :: Rdr user to login...")
                  val result = isAuth.onUnauthBase(request)
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
          LOGGER.debug(s"nodeOwnerInviteAcceptCommon($nodeId, eaId=$eaId): Invalid activation code (eaId): code not found. Expired? $other")
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
  case class Get(override val nodeId: String, override val eaId: String)
                (override val notFoundF: (String, MReq[_]) => Future[Result])
    extends NodeEactAbstract
    with csrf.Get[MNodeEactReq]

  /** Реализация NodeEactBase для CSRF+POST-запросов. */
  case class Post(override val nodeId: String, override val eaId: String)
                 (override val notFoundF: (String, MReq[_]) => Future[Result])
    extends NodeEactAbstract
    with csrf.Post[MNodeEactReq]

}
