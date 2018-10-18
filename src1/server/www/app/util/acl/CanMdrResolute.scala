package util.acl

import io.suggest.model.n2.node.MNodesCache
import io.suggest.req.ReqUtil
import io.suggest.sys.mdr.MMdrResolution
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.req.MNodeReq
import play.api.http.{HttpErrorHandler, Status}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.18 14:37
  * Description: ACL для проверки команды модерации от обычного или необычного юзера.
  */
class CanMdrResolute @Inject()(
                                aclUtil                 : AclUtil,
                                reqUtil                 : ReqUtil,
                                httpErrorHandler        : HttpErrorHandler,
                                mNodesCache             : MNodesCache,
                                implicit private val ec : ExecutionContext,
                              )
  extends MacroLogsImpl
{

  def apply(mdrRes: MMdrResolution): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: MNodeReq[A] => Future[Result]): Future[Result] = {
        val req0 = aclUtil.reqFromRequest(request)
        lazy val logPrefix = s"(${mdrRes.nodeId} u#${req0.user.personIdOpt.orNull})#${System.currentTimeMillis()}:"

        LOGGER.trace(s"$logPrefix Starting for $mdrRes")

        // Запустить получение данных по узлу.
        val mnodeOptFut = mNodesCache.getById( mdrRes.nodeId )

        mnodeOptFut.flatMap {
          // Найден узел, как и ожидалось. Надо понять, есть ли доступ у юзера на модерацию.
          case Some(mnode) =>
            def __runBlock() = block( MNodeReq(mnode, req0, req0.user) )

            if (req0.user.isSuper) {
              // Сразу запустить экшен, т.к. нет смысла
              __runBlock()

            } else {
              // Обычный юзер может модерировать rcvr-размещения на собственных узлах (и под-узлах).
              // Надо понять, подходит ли резолюция под допустимую:
              // - Юзер тут НЕ может модерировать direct-free
              // - Юзер модерирует входящие AdvDirect или по id в рамках типа.
              val nfo = mdrRes.info
              if (nfo.directSelfAll || nfo.directSelfId.nonEmpty) {
                // TODO Недопустимо для обычного юзера модерачить такие размещения.
                ???
              } else
                ???
            }

          //
          case None =>
            LOGGER.warn(s"$logPrefix Node not found: ${mdrRes.nodeId}")
            httpErrorHandler.onClientError(request, Status.NOT_FOUND)
        }
      }

    }
  }

}
