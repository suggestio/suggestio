package util.acl

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.req.ReqUtil
import models.mproj.ICommonDi
import models.req.{MNodeEactReq, MReq}
import models.usr.{EmailActivations, EmailPwIdents}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.15 11:35
 * Description: ActionBuilder'ы для доступа к инвайтам на управление узлом через email.
 * Аддон подмешивается к контроллерам, где необходима поддержка NodeEact.
 */
class NodeEact @Inject() (
                           aclUtil                : AclUtil,
                           emailPwIdents          : EmailPwIdents,
                           emailActivations       : EmailActivations,
                           isAuth                 : IsAuth,
                           reqUtil                : ReqUtil,
                           mCommonDi              : ICommonDi
                         )
  extends MacroLogsImpl
{

  import mCommonDi._


  /** Сборка ActionBuider'ов, проверяющих запросы активации инвайта на узел.
    *
    * @param nodeId id узла, на который клиент пытается получить привелегии.
    * @param eaId id в модели EmailActivation.
    * @param notFoundF Что делать при проблемах?
    *                  $1 - reason
    *                  $2 - sio-реквест.
    */
  def apply(nodeId: String, eaId: String)(notFoundF: (String, MReq[_]) => Future[Result]): ActionBuilder[MNodeEactReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeEactReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeEactReq[A]) => Future[Result]): Future[Result] = {
        val eaOptFut = emailActivations.getById(eaId)
        val nodeOptFut = mNodesCache.getById(nodeId)

        val user = aclUtil.userFromRequest(request)

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
                  case Some(epwIdent) if epwIdent.isVerified && !user.personIdOpt.contains(epwIdent.personId) =>
                    LOGGER.debug(s"eAct has email = ${epwIdent.email}. This is personId[${epwIdent.personId}], but current pwOpt = ${user.personIdOpt.orNull} :: Rdr user to login...")
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
  }

}
