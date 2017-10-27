package io.suggest.ad.edit.srv

import diode.ModelRO
import io.suggest.ad.edit.m.{MAdEditForm, MAdEditFormConf, MAdEditFormInit}
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.routes.routes
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.up.IUploadApi
import play.api.libs.json.Json
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 22:22
  * Description: Интерфейс API сервера для взаимодействия в контексте редактирования рекламной карточки.
  */
trait ILkAdEditApi {

  /** Шаг 1 подготовки к аплоаду файла (картинки, обычно) в контексте карточки.
    *
    * @param file4UpProps Данные файла, готовящегося к заливке.
    * @return Фьючерс с ответом сервера.
    */
  def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp]


  /** Отправка на сервер формы для создания новой карточки.
    *
    * @param producerId id узла-продьюсера.
    * @return
    */
  def saveAdSubmit(producerId: String, form: MAdEditForm): Future[MAdEditFormInit]

}


/** Реализация [[ILkAdEditApi]] поверх HTTP/XHR. */
class LkAdEditApiHttp(
                       confRO    : ModelRO[MAdEditFormConf],
                       uploadApi : IUploadApi
                     )
  extends ILkAdEditApi
{

  import JsRoutes_Controllers_LkAdEdit._

  private def _adProdArgs(): (String, String) = {
    val conf = confRO.value
    val adIdOpt = conf.adId.orNull
    val producerIdOpt = conf.adId.fold(conf.producerId)(_ => null)
    (adIdOpt, producerIdOpt)
  }

  override def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    val conf = confRO.value
    val (adIdNull, producerIdNull) = _adProdArgs
    val route = routes.controllers.LkAdEdit.prepareImgUpload(
      adId   = adIdNull,
      nodeId = producerIdNull
    )
    uploadApi.prepareUpload( route, file4UpProps )
  }


  override def saveAdSubmit(producerId: String, form: MAdEditForm): Future[MAdEditFormInit] = {
    val conf = confRO.value
    val (adIdNull, producerIdNull) = _adProdArgs
    val route = routes.controllers.LkAdEdit.saveAdSubmit(
      adId       = adIdNull,
      producerId = producerIdNull
    )
    val jsonMime = MimeConst.APPLICATION_JSON

    val xhrFut =
      Xhr.successIf200 {
        Xhr.send(
          route = route,
          timeoutMsOpt = Some(13000),
          headers = Seq(
            HttpConst.Headers.CONTENT_TYPE -> jsonMime,
            HttpConst.Headers.ACCEPT -> jsonMime
          ),
          body = Json.toJson(form).toString()
        )
      }

    for (xhr <- xhrFut) yield {
      Json
        .parse( xhr.responseText )
        .as[MAdEditFormInit]
    }
  }

}
