package io.suggest.lk.nodes.form.a

import io.suggest.lk.nodes.{MLknNode, MLknNodeReq, MLknNodeResp}
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

  /** Узнать у сервера подробности по указанному узлу (метаданные, под-узлы).
    *
    * @param nodeId id узла.
    * @return Фьючерс с десериализованным ответом сервера.
    */
  def nodeInfo(nodeId: String): Future[MLknNodeResp]

  /** Создать новый узел на стороне сервера.
    *
    * @param parentId id родительского узла.
    * @param data Данные по создаваемому узлу.
    * @return Фьючерс с ответом по созданному узлу.
    */
  def createSubNodeSubmit(parentId: String, data: MLknNodeReq): Future[MLknNode]


  /** Вызов обновления флага isEnabled для указанного узла.
    *
    * @param nodeId id обновляемого узла.
    * @param isEnabled Новое значение флага isEnabled.
    * @return Фьючерс с обновлёнными данными по обновлённому узлу.
    */
  def setNodeEnabled(nodeId: String, isEnabled: Boolean): Future[MLknNode]

  /** Запуск удаления какого-то узла на сервере.
    *
    * @param nodeId id узла.
    * @return true, если узел был удалён.
    *         false, если узел не был найден.
    *         exception, если всё ещё хуже.
    */
  def deleteNode(nodeId: String): Future[Boolean]

  /** Запуск экшена редактирования узла.
    *
    * @param nodeId id узла.
    * @param data Обновлённые данные по узлу. id игнорируется или должен быть None.
    * @return Фьючерс с инфой по обновлённому узлу.
    */
  def editNode(nodeId: String, data: MLknNodeReq): Future[MLknNode]

}


/** Реализация [[ILkNodesApi]] для взаимодействия с серверным контроллером LkNodes через обычные HTTP-запросы. */
class LkNodesApiHttpImpl extends ILkNodesApi {

  import io.suggest.lk.nodes.form.u.LkNodesRoutes._


  override def nodeInfo(nodeId: String): Future[MLknNodeResp] = {
    Xhr.unBooPickleResp[MLknNodeResp] {
      Xhr.requestBinary(
        route = jsRoutes.controllers.LkNodes.nodeInfo(nodeId)
      )
    }
  }


  override def createSubNodeSubmit(parentId: String, data: MLknNodeReq): Future[MLknNode] = {
    Xhr.unBooPickleResp[MLknNode] {
      Xhr.requestBinary(
        route = jsRoutes.controllers.LkNodes.createSubNodeSubmit(parentId),
        body  = PickleUtil.pickle(data)
      )
    }
  }


  override def setNodeEnabled(nodeId: String, isEnabled: Boolean): Future[MLknNode] = {
    Xhr.unBooPickleResp[MLknNode] {
      Xhr.requestBinary(
        route = jsRoutes.controllers.LkNodes.setNodeEnabled(nodeId, isEnabled)
      )
    }
  }


  override def deleteNode(nodeId: String): Future[Boolean] = {
    import Xhr.Status._
    for {
      resp <- Xhr.successIfStatus( NO_CONTENT, NOT_FOUND ) {
        Xhr.send(
          route = jsRoutes.controllers.LkNodes.deleteNode(nodeId)
        )
      }
    } yield {
      // В логах нередко встречаются null вместо инстансов XHR. Поэтому залезаем в реквест аккуратно.
      resp == null || resp.status == NO_CONTENT
    }
  }


  override def editNode(nodeId: String, data: MLknNodeReq): Future[MLknNode] = {
    Xhr.unBooPickleResp[MLknNode] {
      Xhr.requestBinary(
        route = jsRoutes.controllers.LkNodes.editNode(nodeId),
        body  = PickleUtil.pickle(data)
      )
    }
  }

}
