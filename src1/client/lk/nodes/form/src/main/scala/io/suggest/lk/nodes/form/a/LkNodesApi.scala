package io.suggest.lk.nodes.form.a

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.tf.daily.ITfDailyMode
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknNode, MLknNodeReq, MLknNodeResp}
import io.suggest.proto.http.client.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.univeq._

import scalajs.js.JSConverters._
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
    * @param adId id карточки, для которой происходит размещение.
    * @param onNodeRk rcvrKey запращиваемого узла, либо None - корень дерева.
    * @return Фьючерс с десериализованным ответом сервера.
    */
  def subTree(onNodeRk: Option[RcvrKey] = None, adId: Option[String]): Future[MLknNodeResp]

  /** Создать новый узел на стороне сервера.
    *
    * @param parentRk id родительского узла.
    * @param data Данные по создаваемому узлу.
    * @return Фьючерс с ответом по созданному узлу.
    */
  def createSubNodeSubmit(parentRk: RcvrKey, data: MLknNodeReq): Future[MLknNode]


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
  def setAdvShowOpened(adId: String, isShowOpened: Boolean, onNode: RcvrKey): Future[MLknNode]

  /** Запустить запрос обновления флага AlwaysOutlined, для указанного узла и карточки.
    *
    * @param adId id рекламной карточки.
    * @param isAlwaysOutlined Новое значение флага AlwaysOutlined.
    * @param onNode На каком узле размещение карточки.
    * @return Фьючерс.
    */
  def setAlwaysOutlined(adId: String, isAlwaysOutlined: Boolean, onNode: RcvrKey): Future[MLknNode]

  /** Выставить новый режим тарификации указанного узла.
    *
    * @param onNode Интересующий узел.
    * @param mode Новый режим узла.
    * @return Фьючерс с обновлёнными данными узла.
    */
  def setTfDaily(onNode: RcvrKey, mode: ITfDailyMode): Future[MLknNode]

  /** Сканирование узлов.
    *
    * @param req Данные по id маячков.
    * @return Фьючерс с ответом, где root-узел следует отбросить.
    */
  def beaconsScan(req: MLknBeaconsScanReq): Future[MLknNodeResp]

}


/** Реализация [[ILkNodesApi]] для взаимодействия с серверным контроллером LkNodes через обычные HTTP-запросы. */
final class LkNodesApiHttpImpl(
                                diConfig: NodesDiConf,
                              )
  extends ILkNodesApi
{

  override def subTree(onNodeRk: Option[RcvrKey] = None, adId: Option[String]): Future[MLknNodeResp] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkNodes.subTree(
          onNodeRk = onNodeRk
            .map( _.toJSArray )
            .toUndef,
          adId = adId.toUndef,
        ),
        data = HttpReqData(
          headers = HttpReqData.headersJsonAccept,
          config  = diConfig.httpClientConfig(),
        ),
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MLknNodeResp]
  }


  override def createSubNodeSubmit(parentRk: RcvrKey, data: MLknNodeReq): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.createSubNodeSubmit(
        parentRk = RcvrKey.rcvrKey2urlPath( parentRk ),
      ),
      data = HttpReqData(
        body    = Json.toJson(data).toString(),
        headers = HttpReqData.headersJsonSendAccept,
        config  = diConfig.httpClientConfig(),
      ),
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MLknNode]
  }


  override def setNodeEnabled(nodeId: String, isEnabled: Boolean): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.setNodeEnabled(nodeId, isEnabled),
      data  = HttpReqData(
        headers = HttpReqData.headersJsonAccept,
        config = diConfig.httpClientConfig(),
      ),
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MLknNode]
  }


  override def deleteNode(nodeId: String): Future[Boolean] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.deleteNode(nodeId),
      data = HttpReqData(
        config = diConfig.httpClientConfig(),
      ),
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .flatMap { resp =>
        // Раньше были разные варианты (NoContent/NotFound), но cordova-okhttp-плагин слишком кривой для столь очевидных вещей.
        resp
          .text()
          .map(_.toBoolean)
      }
  }


  override def editNode(nodeId: String, data: MLknNodeReq): Future[MLknNode] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkNodes.editNode(nodeId),
      data = HttpReqData(
        body    = Json.toJson(data).toString(),
        headers = HttpReqData.headersJsonSendAccept,
        config  = diConfig.httpClientConfig(),
      ),
    )
    HttpClient.execute(req)
      .respAuthFut
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
      data = HttpReqData(
        headers = HttpReqData.headersJsonAccept,
        config  = diConfig.httpClientConfig(),
      ),
    )
    HttpClient.execute(req)
      .respAuthFut
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
        headers = HttpReqData.headersJsonSendAccept,
        config  = diConfig.httpClientConfig(),
      )
    )
    HttpClient.execute(req)
      .respAuthFut
      .successIf200
      .unJson[MLknNode]
  }

  override def setAdvShowOpened(adId: String, isShowOpened: Boolean, onNode: RcvrKey): Future[MLknNode] = {
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.LkNodes.setAdvShowOpened(adId, isShowOpened, RcvrKey.rcvrKey2urlPath(onNode)),
          data = HttpReqData(
            config = diConfig.httpClientConfig(),
          ),
        )
      )
      .respAuthFut
      .successIf200
      .unJson[MLknNode]
  }

  override def setAlwaysOutlined(adId: String, isAlwaysOutlined: Boolean, onNode: RcvrKey): Future[MLknNode] = {
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.LkNodes.setAlwaysOutlined(adId, isAlwaysOutlined, RcvrKey.rcvrKey2urlPath(onNode)),
          data = HttpReqData(
            config = diConfig.httpClientConfig(),
          ),
        )
      )
      .respAuthFut
      .successIf200
      .unJson[MLknNode]
  }

  override def beaconsScan(scanReq: MLknBeaconsScanReq): Future[MLknNodeResp] = {
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.LkNodes.beaconsScan(
            PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject( scanReq ) )
          ),
          data = HttpReqData(
            headers = HttpReqData.headersJsonAccept,
            config  = diConfig.httpClientConfig(),
            // Короткий таймаут, т.к. данные скана могут слишком сильно устареть в ожидании ответа.
            timeoutMs = Some( 5000 ),
            cache = MHttpCacheInfo(
              policy = MHttpCachingPolicies.NetworkFirst,
            ),
          ),
        )
      )
      .respAuthFut
      .successIf200
      .unJson[MLknNodeResp]
  }

}
