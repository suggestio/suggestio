package util.acl

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.adv.MExtTargets
import models.req.{MExtTargetNodeReq, MReq}
import play.api.mvc._
import io.suggest.req.ReqUtil
import models.mproj.ICommonDi
import play.api.http.Status

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 17:34
 * Description: ActionBuilder для экшенов доступа к записям [[models.adv.MExtTarget]] по id.
 */

class CanAccessExtTarget @Inject() (
                                     aclUtil            : AclUtil,
                                     mExtTargets        : MExtTargets,
                                     isNodeAdmin        : IsNodeAdmin,
                                     reqUtil            : ReqUtil,
                                     mCommonDi          : ICommonDi
                                   )
  extends MacroLogsImpl
{ outer =>

  import mCommonDi._


  /** @param tgId id ранее сохранённого экземпляра [[models.adv.MExtTarget]]. */
  def apply(tgId: String): ActionBuilder[MExtTargetNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MExtTargetNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MExtTargetNodeReq[A]) => Future[Result]): Future[Result] = {
        val tgOptFut = mExtTargets.getById(tgId)

        val user = aclUtil.userFromRequest(request)

        tgOptFut.flatMap {
          // Запрошенная цель существует. Нужно проверить права на её узел.
          case Some(tg) =>
            val adnNodeOptFut = isNodeAdmin.isAdnNodeAdmin(tg.adnId, user)

            adnNodeOptFut.flatMap {
              // У юзера есть права на узел. Запускаем экшен на исполнение.
              case Some(mnode) =>
                val req1 = MExtTargetNodeReq(tg, mnode, request, user)
                block(req1)

              // Нет прав на узел.
              case None =>
                val req1 = MReq(request, user)
                isNodeAdmin.onUnauthNode(req1)
            }

          case None =>
            LOGGER.info(s"User#${user.personIdOpt.orNull} tried to access to ExtTarget[$tgId], but id does not exist.")
            tgNotFound(request)
        }
      }

      /** Что делать и что возвращать юзеру, если цель не найдена? */
      def tgNotFound(request: Request[_]): Future[Result] =
        errorHandler.onClientError( request, Status.NOT_FOUND, s"Target does not exist: $tgId")

    }
  }

}
