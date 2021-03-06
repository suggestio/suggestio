package io.suggest.lk.nodes.form.a

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.tf.daily.ITfDailyMode
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknNode, MLknNodeReq, MLknNodeResp, MLknModifyQs}
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

  /** Обновление узла согласно переданным данным.
    * @param qs Контейнер параметров запроса.
    * @return Описание обновлённого узла.
    */
  def modifyNode(qs: MLknModifyQs): Future[MLknNode]

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

  override def modifyNode(qs: MLknModifyQs): Future[MLknNode] = {
    HttpClient
      .execute(
        HttpReq.routed(
          route = routes.controllers.LkNodes.modifyNode(
            PlayJsonSjsUtil.toNativeJsonObj(
              Json.toJsObject( qs ) )
          ),
          data = HttpReqData(
            headers   = HttpReqData.headersJsonAccept,
            timeoutMs = Some( 7000 ),
            config    = diConfig.httpClientConfig(),
          ),
        )
      )
      .respAuthFut
      .successIf200
      .unJson[MLknNode]
  }

}
