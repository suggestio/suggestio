package io.suggest.lk.nodes.form.a

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.tf.daily.ITfDailyMode
import io.suggest.lk.nodes.{MLknNode, MLknNodeReq, MLknNodeResp}
import io.suggest.proto.HttpConst
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.{HttpReq, HttpReqData, Xhr}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json
import japgolly.univeq._

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

  /** Запустить экшен обновления прямого размещения на узле.
    *
    * @param adId id текущей карточки (берётся из конфига).
    * @param isEnabled Разместить или снять с размещения?
    * @param onNode RcvrKey узла, на котором происходит размещение.
    * @return Фьючерс с обновлённой инфой по узлу.
    */
  def setAdv(adId: String, isEnabled: Boolean, onNode: RcvrKey): Future[MLknNode]

  /** Запустить обновление флага showOpened для указанного узла и карточки.
    *
    * @param adId id рекламной карточки.
    * @param isShowOpened Новое значение isShowOpened.
    * @param onNode На каком узле действо разворачивать при открытии?
    * @return Фьючерс с обнволённым узлом.
    */
  def setAdvShowOpened(adId: String, isShowOpened: Boolean, onNode: RcvrKey): Future[_]

  /** Выставить новый режим тарификации указанного узла.
    *
    * @param onNode Интересующий узел.
    * @param mode Новый режим узла.
    * @return Фьючерс с обновлёнными данными узла.
    */
  def setTfDaily(onNode: RcvrKey, mode: ITfDailyMode): Future[MLknNode]

}


/** Реализация [[ILkNodesApi]] для взаимодействия с серверным контроллером LkNodes через обычные HTTP-запросы. */
class LkNodesApiHttpImpl extends ILkNodesApi {

  override def nodeInfo(nodeId: String): Future[MLknNodeResp] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.nodeInfo(nodeId),
      data  = HttpReqData.justAcceptJson
    )
    Xhr.execute( req )
      .successIf200
      .unJson[MLknNodeResp]
  }


  override def createSubNodeSubmit(parentId: String, data: MLknNodeReq): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.createSubNodeSubmit(parentId),
      data = HttpReqData(
        body    = Json.toJson(data).toString(),
        headers = HttpReqData.headersJsonSendAccept
      )
    )
    Xhr.execute( req )
      .successIf200
      .unJson[MLknNode]
  }


  override def setNodeEnabled(nodeId: String, isEnabled: Boolean): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.setNodeEnabled(nodeId, isEnabled),
      data  = HttpReqData.justAcceptJson
    )
    Xhr.execute( req )
      .successIf200
      .unJson[MLknNode]
  }


  override def deleteNode(nodeId: String): Future[Boolean] = {
    import io.suggest.proto.HttpConst.Status._
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.deleteNode(nodeId)
    )
    Xhr.execute( req )
      .successIfStatus( NO_CONTENT, NOT_FOUND )
      .responseStatusFut
      .map { status =>
        status ==* NO_CONTENT
      }
  }


  override def editNode(nodeId: String, data: MLknNodeReq): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.editNode(nodeId),
      data = HttpReqData(
        body    = Json.toJson(data).toString(),
        headers = HttpReqData.headersJsonSendAccept
      )
    )
    Xhr.execute(req)
      .successIf200
      .unJson[MLknNode]
  }


  override def setAdv(adId: String, isEnabled: Boolean, onNode: RcvrKey): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.setAdv(
        adId          = adId,
        isEnabled     = isEnabled,
        onNodeRcvrKey = RcvrKey.rcvrKey2urlPath( onNode )
      ),
      data = HttpReqData.justAcceptJson
    )
    Xhr.execute(req)
      .successIf200
      .unJson[MLknNode]
  }


  override def setTfDaily(onNode: RcvrKey, mode: ITfDailyMode): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.setTfDaily(
        onNodeRcvrKey = RcvrKey.rcvrKey2urlPath( onNode )
      ),
      data = HttpReqData(
        body    = Json.toJson(mode).toString(),
        headers = HttpReqData.headersJsonSendAccept
      )
    )
    Xhr.execute(req)
      .successIf200
      .unJson[MLknNode]
  }

  override def setAdvShowOpened(adId: String, isShowOpened: Boolean, onNode: RcvrKey): Future[_] = {
    val S = HttpConst.Status
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.setAdvShowOpened(adId, isShowOpened, RcvrKey.rcvrKey2urlPath(onNode))
    )
    Xhr.execute( req )
      .successIfStatus( S.OK, S.NO_CONTENT )
      .responseRawFut
  }

}
