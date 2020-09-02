package io.suggest.ad.edit.srv

import diode.ModelRO
import io.suggest.ad.edit.m.{MAdEditFormConf, MAdEditFormInit}
import io.suggest.proto.http.client.HttpClient
import io.suggest.jd.MJdData
import io.suggest.proto.http.model._
import io.suggest.routes.{PlayRoute, routes}
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 22:22
  * Description: Интерфейс API сервера для взаимодействия в контексте редактирования рекламной карточки.
  */
trait ILkAdEditApi {

  /** Отправка на сервер формы для создания новой карточки.
    *
    * @param producerId id узла-продьюсера.
    * @return
    */
  def saveAdSubmit(producerId: String, adData: MJdData): Future[MAdEditFormInit]

  /** Удалить карточку.
    *
    * @param adId id удалённой карточки.
    * @return Фьючерс с URL для редиректа пользователя.
    */
  def deleteSubmit(adId: String): Future[String]

}


/** Реализация [[ILkAdEditApi]] поверх HTTP/XHR. */
class LkAdEditApiHttp(
                       confRO    : ModelRO[MAdEditFormConf],
                     )
  extends ILkAdEditApi
{

  private def XHR_REQ_TIMEOUT_MS_OPT = Some(15000)

  private def _adProdArgs(): (String, String) = {
    val conf = confRO.value
    val adIdOpt = conf.adId.orNull
    val producerIdOpt = conf.adId.fold(conf.producerId)(_ => null)
    (adIdOpt, producerIdOpt)
  }

  def prepareUploadRoute: PlayRoute = {
    val (adIdNull, producerIdNull) = _adProdArgs()
    routes.controllers.LkAdEdit.prepareImgUpload(
      adId   = adIdNull,
      nodeId = producerIdNull
    )
  }

  override def saveAdSubmit(producerId: String, form: MJdData): Future[MAdEditFormInit] = {
    val (adIdNull, producerIdNull) = _adProdArgs()
    val route = routes.controllers.LkAdEdit.saveAdSubmit(
      adId       = adIdNull,
      producerId = producerIdNull
    )

    val req = HttpReq.routed(
      route = route,
      data  = HttpReqData(
        timeoutMs = XHR_REQ_TIMEOUT_MS_OPT,
        headers   = HttpReqData.headersJsonSendAccept,
        body      = Json.toJson(form).toString()
      )
    )

    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MAdEditFormInit]
  }


  override def deleteSubmit(adId: String): Future[String] = {
    val adId = confRO.value.adId.get
    val route = routes.controllers.LkAdEdit.deleteSubmit( adId )
    val req = HttpReq.routed(
      route = route,
      data  = HttpReqData(
        timeoutMs = XHR_REQ_TIMEOUT_MS_OPT
      )
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .responseTextFut
  }

}
