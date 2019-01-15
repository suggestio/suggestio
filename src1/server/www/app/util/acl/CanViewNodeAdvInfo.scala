package util.acl

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.req.{IAdProdReq, MNodeMaybeAdminReq}
import play.api.mvc._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.es.model.EsModel
import io.suggest.model.n2.node.MNodes
import io.suggest.req.ReqUtil
import play.api.http.{HttpErrorHandler, Status}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 18:35
  * Description: ACL проверки возможности получения юзером данных по какому-либо узлу по его id.
  * Можно
  */
class CanViewNodeAdvInfo @Inject() (
                                     esModel                    : EsModel,
                                     mNodes                     : MNodes,
                                     aclUtil                    : AclUtil,
                                     canAdvAd                   : CanAdvAd,
                                     isAuth                     : IsAuth,
                                     isNodeAdmin                : IsNodeAdmin,
                                     dab                        : DefaultActionBuilder,
                                     reqUtil                    : ReqUtil,
                                     httpErrorHandler           : HttpErrorHandler,
                                     implicit private val ec    : ExecutionContext,
                                   )
  extends MacroLogsImpl
{ outer =>

  import esModel.api._

  /** Вся ACL-логика живёт здесь.
    *
    * @param nodeId id узла.
    * @param forAdIdOpt Опциональный id рекламной карточки, если экшен в контексте какой-то карточки.
    * @param request0 Исходный реквест.
    * @param f Тело экшена.
    * @return Фьючерс с HTTP-ответом.
    */
  private def _apply[A](nodeId: String, forAdIdOpt: Option[String], request0: Request[A])
                       (f: MNodeMaybeAdminReq[A] => Future[Result]): Future[Result] = {

    val request = aclUtil.reqFromRequest( request0 )
    val user = aclUtil.userFromRequest( request )
    lazy val logPrefix = s"${outer.getClass.getSimpleName}($nodeId${forAdIdOpt.fold("")("," + _)}):"

    user.personIdOpt.fold {
      LOGGER.debug(s"$logPrefix Refused anonymous user from ${request.remoteClientAddress}")
      isAuth.onUnauth(request)

    } { personId =>
      // Юзер залогинен. В фоне запускаем чтение запрошенного узла...
      val mnodeOptFut = mNodes.getByIdCache(nodeId)

      // Ответ при проблемах с доступом для залогиненного юзера всегда один:
      def forbidden: Future[Result] =
        httpErrorHandler.onClientError( request, Status.FORBIDDEN, s"No access to node $nodeId.")

      // Запустить в фоне проверку доступа к опциональной карточке.
      val madProdReqOptFut = forAdIdOpt.fold [Future[Either[Future[Result], Option[IAdProdReq[A]]]]] {
        LOGGER.trace(s"$logPrefix adId undefined, skipped.")
        Right(None)
      } { forAdId =>
        for {
          madReqOpt <- canAdvAd.maybeAllowed( forAdId, request )
        } yield {
          madReqOpt.fold [Either[Future[Result], Option[IAdProdReq[A]]]] {
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
  def apply(nodeId: String, forAdIdOpt: Option[String] = None): ActionBuilder[MNodeMaybeAdminReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeMaybeAdminReq] {
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
    dab.async(action.parser) { request =>
      _apply(nodeId, forAdIdOpt, request)(action.apply)
    }
  }

}
