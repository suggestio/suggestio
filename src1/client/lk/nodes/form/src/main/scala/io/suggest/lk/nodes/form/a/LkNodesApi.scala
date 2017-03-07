package io.suggest.lk.nodes.form.a

import io.suggest.lk.nodes.{ILknTreeNode, MLknNodeReq, MLknNodeResp}
import io.suggest.lk.router.jsRoutes
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 21:41
  * Description: API для взаимодействия с контроллером LkNodes.
  */
trait ILkNodesApi {

  /** Узнать под-узлы для указанного узла.
    *
    * @param nodeId id узла.
    * @return Фьючерс с десериализованным ответом сервера.
    */
  def subNodesOf(nodeId: String): Future[MLknNodeResp]

  /** Создать новый узел на стороне сервера.
    *
    * @param parentId id родительского узла.
    * @param data Данные по создаваемому узлу.
    * @return Фьючерс с ответом по созданному узлу.
    */
  def createSubNodeSubmit(parentId: String, data: MLknNodeReq): Future[ILknTreeNode]

}


/** Реализация [[ILkNodesApi]] для взаимодействия с серверным контроллером LkNodes через обычные HTTP-запросы. */
class LkNodesApiHttpImpl extends ILkNodesApi {

  import io.suggest.lk.nodes.form.u.LkNodesRoutes._


  override def subNodesOf(nodeId: String): Future[MLknNodeResp] = {
    for {
      resp <- Xhr.requestBinary(
        route = jsRoutes.controllers.LkNodes.subNodesOf(nodeId)
      )
    } yield {
      PickleUtil.unpickle[MLknNodeResp](resp)
    }
  }


  override def createSubNodeSubmit(parentId: String, data: MLknNodeReq): Future[ILknTreeNode] = {
    for {
      resp <- Xhr.requestBinary(
        route = jsRoutes.controllers.LkNodes.createSubNodeSubmit(parentId),
        body  = PickleUtil.pickle(data)
      )
    } yield {
      PickleUtil.unpickle[ILknTreeNode](resp)
    }
  }

}
