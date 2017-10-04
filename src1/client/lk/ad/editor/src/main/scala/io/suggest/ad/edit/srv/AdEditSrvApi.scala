package io.suggest.ad.edit.srv

import diode.ModelRO
import io.suggest.ad.edit.m.MAdEditFormConf
import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.lk.router.jsRoutes
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.ext.AjaxException
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 22:22
  * Description: Интерфейс API сервера для взаимодействия в контексте редактирования рекламной карточки.
  */
trait IAdEditSrvApi {

  /** Шаг 1 подготовки к аплоаду файла (картинки, обычно) в контексте карточки.
    *
    * @param file4UpProps Данные файла, готовящегося к заливке.
    * @return Фьючерс с ответом сервера.
    */
  def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp]

}


/** Реализация [[IAdEditSrvApi]] поверх HTTP. */
class AdEditSrvApiHttp( confRO: ModelRO[MAdEditFormConf] ) extends IAdEditSrvApi {

  import JsRoutes_Controllers_LkAdEdit._


  override def prepareUpload(file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    val conf = confRO.value
    val route = jsRoutes.controllers.LkAdEdit.prepareImgUpload(
      conf.adId.orNull,
      nodeId = conf.adId.fold(conf.producerId)(_ => null)
    )
    val propsJson = Json.toJson(file4UpProps).toString()

    for {
      respJson <- {
        val H = HttpConst.Headers
        val applicationJson = MimeConst.APPLICATION_JSON
        Xhr.send(
          route = route,
          headers = Seq(
            H.ACCEPT        -> applicationJson,
            H.CONTENT_TYPE  -> applicationJson    // TODO + "; charset=utf8" ?
          ),
          body = propsJson
        )
          .map { xhr  =>
            xhr.responseText
          }
          .recover {
            // Сервер разные коды прислывает, но мы сами коды игнорим, важен - контент.
            case aex: AjaxException if aex.xhr.status == HttpConst.Status.NOT_ACCEPTABLE =>
              aex.xhr.response.asInstanceOf[String]
          }
      }
    } yield {
      // Распарсить ответ. 20х и 406 содержат body в одинаковом формате.
      Json
        .parse(respJson)
        .as[MUploadResp]
    }
  }

}
