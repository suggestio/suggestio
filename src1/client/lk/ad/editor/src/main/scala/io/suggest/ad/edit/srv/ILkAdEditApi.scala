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
  def createAdSubmit(producerId: String, form: MAdEditForm): Future[MAdEditFormInit]

}


/** Реализация [[ILkAdEditApi]] поверх HTTP/XHR. */
class LkAdEditApiHttp(
                       confRO    : ModelRO[MAdEditFormConf],
                       uploadApi : IUploadApi
                     )
  extends ILkAdEditApi
{

  import JsRoutes_Controllers_LkAdEdit._


  override def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    val conf = confRO.value
    val route = routes.controllers.LkAdEdit.prepareImgUpload(
      adId   = conf.adId.orNull,
      nodeId = conf.adId.fold(conf.producerId)(_ => null)
    )
    uploadApi.prepareUpload( route, file4UpProps )
  }


  override def createAdSubmit(producerId: String, form: MAdEditForm): Future[MAdEditFormInit] = {
    val conf = confRO.value
    val route = routes.controllers.LkAdEdit.createAdSubmit( conf.producerId )
    val jsonMime = MimeConst.APPLICATION_JSON

    val xhrFut =
      Xhr.successIfStatus(HttpConst.Status.CREATED) {
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
