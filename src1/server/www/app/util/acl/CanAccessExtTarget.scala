package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.adv.MExtTargets
import models.req.{MExtTargetNodeReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 17:34
 * Description: ActionBuilder для экшенов доступа к записям [[models.adv.MExtTarget]] по id.
 */

class CanAccessExtTarget @Inject() (
                                     mExtTargets        : MExtTargets,
                                     isNodeAdmin        : IsNodeAdmin,
                                     mCommonDi          : ICommonDi
                                   )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{ outer =>

  import mCommonDi._


  /** @param tgId id ранее сохранённого экземпляра [[models.adv.MExtTarget]]. */
  def apply(tgId: String): ActionBuilder[MExtTargetNodeReq] = {
    new SioActionBuilderImpl[MExtTargetNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MExtTargetNodeReq[A]) => Future[Result]): Future[Result] = {
        val tgOptFut = mExtTargets.getById(tgId)

        val personIdOpt = sessionUtil.getPersonId(request)

        tgOptFut.flatMap {
          // Запрошенная цель существует. Нужно проверить права на её узел.
          case Some(tg) =>
            val user = mSioUsers(personIdOpt)
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
            LOGGER.info(s"User $personIdOpt tried to access to ExtTarget[$tgId], but id does not exist.")
            tgNotFound(request)
        }
      }

      /** Что делать и что возвращать юзеру, если цель не найдена? */
      def tgNotFound(request: Request[_]): Future[Result] = {
        Results.NotFound("Target does not exist: " + tgId)
      }

    }
  }

}
