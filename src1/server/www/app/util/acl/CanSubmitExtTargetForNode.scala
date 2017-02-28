package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.adv._
import models.mproj.ICommonDi
import models.req.{ISioUser, MNodeExtTgSubmitReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import util.adv.ext.AdvExtFormUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.fut.FutureUtil.HellImplicits._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.15 16:47
 * Description: Файл содержит ACL ActionBuilder для сабмита экземпляров данных в [[models.adv.MExtTarget]].
 * Это первый ActionBuilder в проекте, залезающий в тело реквеста с помощью маппинга формы.
 * В теле может содержаться id экшена, и нужно проверять права доступа.
 */


/** Аддон для контроллера для добавления поддержки */
class CanSubmitExtTargetForNode @Inject() (
                                            advExtFormUtil          : AdvExtFormUtil,
                                            mExtTargets             : MExtTargets,
                                            isAdnNodeAdmin          : IsAdnNodeAdmin,
                                            mCommonDi               : ICommonDi
                                          )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  import mCommonDi._


  /** ActionBuilder проверки доступа на запись (create, edit) для target'а,
    * id которого возможно задан в теле POST'а.
    *
    * @param nodeId id узла, заявленного клиентом.
    */
  def apply(nodeId: String): ActionBuilder[MNodeExtTgSubmitReq] = {
    new SioActionBuilderImpl[MNodeExtTgSubmitReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeExtTgSubmitReq[A]) => Future[Result]): Future[Result] = {
        val personIdOpt = sessionUtil.getPersonId(request)
        val user = mSioUsers(personIdOpt)

        val isAdnNodeAdmFut = isAdnNodeAdmin.isAdnNodeAdmin(nodeId, user)
        val formBinded = advExtFormUtil.oneTargetFullFormM(nodeId).bindFromRequest()(request)

        // Запускаем сразу в фоне поиск уже сохранённой цели.
        val tgIdOpt = formBinded.apply("id").value
        val tgOptFut = FutureUtil.optFut2futOpt(tgIdOpt) { tgId =>
          mExtTargets.getById(tgId)
        }

        isAdnNodeAdmFut.flatMap {
          // Юзер является админом текущего узла. Нужно проверить права на цель.
          case Some(mnode) =>
            // Всё ок может быть в разных случаях, Общий код вынесен сюда.
            def allOk(tgOpt: Option[MExtTarget] = None): Future[Result] = {
              val req1 = MNodeExtTgSubmitReq(mnode, formBinded, tgOpt, request, user)
              block(req1)
            }
            tgOptFut.flatMap {
              // Цель не существует...
              case None =>
                if (tgIdOpt.isDefined)
                  // но если id цели передан, то это ненормально
                  LOGGER.debug(s"User[$personIdOpt] submitted tg_id[${tgIdOpt.get}] for inexisting ext target. Tolerating...")
                allOk()

              // Есть такая цель в хранилищах.
              case someTg @ Some(tg) =>
                if (tg.adnId == nodeId) {
                  // Эта цель принадлежит узлу, которым владеет текущий юзер.
                  allOk(someTg)
                } else {
                  // [xakep] Ксакеп отакует: попытка перезаписать чужую цель.
                  breakInAttempted(user, request, tg)
                }
            }

          // Нет прав на узел.
          case None =>
            val req1 = MReq(request, user)
            isAdnNodeAdmin.onUnauthNode(req1)
        }
      }

      def breakInAttempted(user: ISioUser, request: Request[_], tg: MExtTarget): Future[Result] = {
        LOGGER.warn(s"FORBIDDEN: User[${user.personIdOpt}] @${request.remoteAddress} tried to rewrite foreign target[${tg.id.get}] via node[$nodeId]. AdnNode expected = ${tg.adnId}.")
        Results.Forbidden(s"Target ${tg.id.get} doesn't belongs to node[$nodeId].")
      }

    }
  }

}
