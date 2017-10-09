package io.suggest.up

import io.suggest.file.up.{MFile4UpProps, MUploadResp}
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.ext.AjaxException
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 14:41
  * Description: Утиль для аплоада файлов на сервер.
  */
object UploadUtil {

  /** Код тела подготовки к аплоаду и декодинга результата по HTTP.
    *
    * @param route Роута.
    * @param file4UpProps Данные файла.
    * @return Фьючерс с ответом сервера.
    */
  def prepareUploadXhr(route: Route, file4UpProps: MFile4UpProps): Future[MUploadResp] = {
    for {
      // Запустить XHR...
      respJson <- {
        val H = HttpConst.Headers
        val applicationJson = MimeConst.APPLICATION_JSON
        Xhr.send(
          route = route,
          headers = Seq(
            H.ACCEPT        -> applicationJson,
            H.CONTENT_TYPE  -> applicationJson    // TODO + "; charset=utf8" ?
          ),
          body = Json.toJson(file4UpProps).toString()
        )
          .map { xhr  =>
            xhr.responseText
          }
          // 20х и 406 содержат body в одинаковом формате. Перехватить HTTP Not acceptable:
          .recover {
            // Сервер разные коды прислывает, но мы сами коды игнорим, важен - контент.
            case aex: AjaxException if aex.xhr.status == HttpConst.Status.NOT_ACCEPTABLE =>
              aex.xhr.response.asInstanceOf[String]
          }
      }
    } yield {
      // Распарсить ответ.
      Json
        .parse(respJson)
        .as[MUploadResp]
    }
  }

}

