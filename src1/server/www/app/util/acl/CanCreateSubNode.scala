package util.acl

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.err.HttpResultingException
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.MNodesChainReq
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.2020 12:08
  * Description: Проверка юзера на предмет возможности создания под-узла.
  */
final class CanCreateSubNode(
                              injector: Injector,
                            )
  extends MacroLogsImpl
{

  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Сборка ActionBuilder для проверки прав на добавления под-узла.
    *
    * @param parentRk RcvrKey до узла.
    * @return ActionBuilder.
    */
  def apply(parentRk: RcvrKey): ActionBuilder[MNodesChainReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodesChainReq] {

      override def invokeBlock[A](request: Request[A], block: MNodesChainReq[A] => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest( request )
        lazy val logPrefix = s"(${parentRk.mkString("/")}): User#${user.personIdOpt.orNull} "

        for {
          // Проверить admin-права на цепочку узлов:
          nodesChainOpt <- isNodeAdmin.isNodeChainAdmin( parentRk, user )
          nodesChain = nodesChainOpt getOrElse {
            LOGGER.warn(s"$logPrefix is NOT a node-admin.")
            throw HttpResultingException( isAuth.onUnauth(request) )
          }

          // Убедиться, что в last-узел есть возможность добавления под-узла.
          mreq = MNodesChainReq( nodesChain, request, user )

          // Проверить возможность добавления под-узла для последнего узла в цепочке.
          if {
            val lastParent = mreq.mnode
            val r = lastParent.common.ntype.userCanCreateSubNodes
            if (!r) {
              LOGGER.warn(s"$logPrefix is nodes-admin, but parent-node type#${lastParent.common.ntype} prohibits children creation.")
              throw HttpResultingException( isAuth.onUnauth(request) )
            }
            r
          }

          // Рендер результата:
          res <- block(mreq)
        } yield {
          res
        }
      }

    }
  }

}
