package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.{IAdProdReq, MNodeMaybeAdminReq}
import play.api.mvc._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 18:35
  * Description: ACL проверки возможности получения юзером данных по какому-либо узлу по его id.
  * Можно
  */
class CanViewNodeAdvInfo @Inject() (
                                     aclUtil                : AclUtil,
                                     canAdvAd               : CanAdvAd,
                                     isAuth                 : IsAuth,
                                     isNodeAdmin            : IsNodeAdmin,
                                     mCommonDi              : ICommonDi
                                   )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{ outer =>

  import mCommonDi.{ec, mNodesCache}


  /** Вся ACL-логика живёт здесь.
    *
    * @param nodeId id узла.
    * @param forAdIdOpt Опциональный id рекламной карточки, если экшен в контексте какой-то карточки.
    * @param request реквест.
    * @param f Тело экшена.
    * @return Фьючерс с HTTP-ответом.
    */
  private def _apply[A](nodeId: String, forAdIdOpt: Option[String], request: Request[A])
                       (f: MNodeMaybeAdminReq[A] => Future[Result]): Future[Result] = {

    val mreq0 = aclUtil.reqFromRequest( request )
    val user = aclUtil.userFromRequest( mreq0 )
    lazy val logPrefix = s"${outer.getClass.getSimpleName}($nodeId${forAdIdOpt.fold("")("," + _)}):"

    user.personIdOpt.fold {
      LOGGER.debug(s"$logPrefix Refused anonymous user from ${request.remoteAddress}")
      isAuth.onUnauth(request)

    } { personId =>
      // Юзер залогинен. В фоне запускаем чтение запрошенного узла...
      val mnodeOptFut = mNodesCache.getById(nodeId)

      // Ответ при проблемах с доступом для залогиненного юзера всегда один:
      def forbidden: Result = {
        Results.Forbidden( s"No access to node $nodeId." )
      }

      // Запустить в фоне проверку доступа к опциональной карточке.
      val madProdReqOptFut = forAdIdOpt.fold [Future[Either[Result, Option[IAdProdReq[A]]]]] {
        LOGGER.trace(s"$logPrefix adId undefined, skipped.")
        Right(None)
      } { forAdId =>
        for {
          madReqOpt <- canAdvAd.maybeAllowed( forAdId, mreq0 )
        } yield {
          madReqOpt.fold [Either[Result, Option[IAdProdReq[A]]]] {
            LOGGER.warn(s"$logPrefix User#$personId has NO access to ad#$forAdId")
            Left( forbidden )
          } { adProdReq =>
            LOGGER.trace(s"$logPrefix User#$personId allowed to adv $forAdId")
            Right( Some(adProdReq) )
          }
        }
      }

      mnodeOptFut.flatMap {
        // Узел обнаружен. Проверить права RO-доступа к его инфе в общих чертах.
        case Some(mnode) =>
          val userIsAdmin = isNodeAdmin.isNodeAdminCheck(mnode, user)

          // Узел должен иметь тип, подразумевающий публичность информации о самом себе.
          val isNodePublic = mnode.common.ntype.publicCanReadInfoAboutAdvOn

          // Проверить тип узла и его текущую публичность.
          val isAllowed = isNodePublic &&
            // Узел должен быть активен (что необязательо для админа этого узла)
            (userIsAdmin || mnode.common.isEnabled)

          if (isAllowed) {

            // Переходим к проверке опциональной карточки.
            madProdReqOptFut.flatMap {
              // Разрешено всё. Пропускаем реквест вперёд.
              case Right( adProdReqOpt ) =>
                val req1 = MNodeMaybeAdminReq(mnode, userIsAdmin, adProdReqOpt, request, user)
                f(req1)

              case Left( result ) =>
                result
            }

          } else {
            LOGGER.warn(s"$logPrefix User#$personId cannot access to existing node $nodeId. nodePublic?$isNodePublic nodeEnabled?${mnode.common.isEnabled}")
            forbidden
          }

        // Запрошенного узла не найдено в базе. Косим, будто бы к узлу нет доступа.
        case None =>
          LOGGER.warn(s"$logPrefix User#$personId requested access to node, that does NOT exist")
          forbidden
      }

    }
  }


  /** Сборка ActionBuilder'а под указанные аргументы.
    *
    * @param nodeId id интересующего узла.
    * @return ActionBuilder.
    */
  def apply(nodeId: String, forAdIdOpt: Option[String] = None): ActionBuilder[MNodeMaybeAdminReq] = {
    new SioActionBuilderImpl[MNodeMaybeAdminReq] {
      override def invokeBlock[A](request: Request[A], block: (MNodeMaybeAdminReq[A]) => Future[Result]): Future[Result] = {
        _apply(nodeId, forAdIdOpt, request)(block)
      }
    }
  }

  /** Завернуть произвольный экшен в текущую проверку.
    *
    * @param nodeId id узла.
    * @param action экшен.
    * @tparam A тип request body.
    * @return Обновлённый экшен.
    */
  def A[A](nodeId: String, forAdIdOpt: Option[String] = None)(action: Action[A]): Action[A] = {
    Action.async(action.parser) { request =>
      _apply(nodeId, forAdIdOpt, request)(action.apply)
    }
  }

}
