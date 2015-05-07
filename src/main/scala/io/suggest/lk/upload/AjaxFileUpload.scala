package io.suggest.lk.upload

import io.suggest.sjs.common.model.ex.XhrFailedException
import io.suggest.sjs.common.util.ISjsLogger
import org.scalajs.dom.{File, FormData}
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.jquery._

import scala.concurrent.{Promise, Future}
import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.05.15 14:14
 * Запуск аплоада файла на сервер по первому требованию. Параметры аплоада извлекаются из аттрибутов инпута.
 */
trait AjaxFileUpload extends IHandleFile4Upload with ISjsLogger {

  /** Тип распарсенного ответа сервера, который будет передан в _fileUploadSuccess(). */
  type FileUploadRespT <: Any

  /** Значение dataType для $.ajax(). Используется для подбора парсера ответа сервера. */
  protected def _fileUploadAjaxRespDataType: String

  /** Необязательный постпроцессинг параметров ajax-запроса для аплоада. */
  protected def _fileUploadAjaxArgsPp(ajaxSettings: JQueryAjaxSettings, input: HTMLInputElement, cont: JQuery): JQueryAjaxSettings = {
    ajaxSettings
  }

  /**
   * Юзер выбрал файл для загрузки.
   * Нужно отправить файл на сервер и отработать ответ сервера.
   * @param file Файл.
   * @param input input file.
   * @param cont Статический контейнер, на который можно навешивать дополнительные данные.
   * @param e текущее событие change на инпуте.
   */
  protected def _handleFile4Upload(file: File, input: HTMLInputElement, cont: JQuery, e: JQueryEventObject): Future[_] = {
    val p = Promise[Unit]()
    try {
      // Сформировать запрос
      val formData = new FormData()
      formData.append(input.name, file)
      val ajaxSettingsJson = Dictionary[Any](
        "url"         -> input.getAttribute("data-action"),
        "method"      -> "POST",
        "data"        -> formData,
        "contentType" -> false,   // Браузер тогда выставит правильное значение boundary или не выставит вообще.
        "processData" -> false,
        "dataType"    -> _fileUploadAjaxRespDataType,
        // TODO Добавить необязательный прогресс: http://www.dave-bond.com/blog/2010/01/JQuery-ajax-progress-HMTL5/
        "success"     -> { (resp: FileUploadRespT, textStatus: String, jqXhr: JQueryXHR) =>
          p completeWith _fileUploadSuccess(resp, jqXhr, input, cont, e)
        },
        "error"       -> { (jqXHR: JQueryXHR, textStatus: String, errorThrow: String) =>
          p failure XhrFailedException(jqXHR, errorThrow)
        }
      )
      var ajaxSettings = ajaxSettingsJson.asInstanceOf[JQueryAjaxSettings]
      ajaxSettings = _fileUploadAjaxArgsPp(ajaxSettings, input, cont)
      jQuery.ajax(ajaxSettings)

    } catch {
      case ex: Throwable =>
        error("Failed to upload file w/ajax", ex)
        p failure ex
    }

    // Вернуть асинхронный результат.
    p.future
  }

  /** Обработка полученного положительного ответа от сервера по выполненному upload'у. */
  def _fileUploadSuccess(resp: FileUploadRespT, jqXhr: JQueryXHR, input: HTMLInputElement, cont: JQuery,
                         e: JQueryEventObject): Future[Unit]

}
