package io.suggest.lk.upload

import io.suggest.js.UploadConstants
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.jquery.{JQuery, JQueryAjaxSettings}
import scala.concurrent.Future
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.05.15 15:29
 * Description:
 * Если требуется галерея в форме в рамках одного инпута, то значит нужен аплоад несколько файлов
 * (объектов, изображений, etc).
 * И нужно формировать id имен на лету с помощью счетчика в ATTR_MULTI_INDEX_COUNTER аттрибуте.
 * Аттрибут инициализивуется последним индексом при рендере шаблона, если требуется галерея.
 *
 * Ссылка для аплоада перед каждым POST-запросом дополняется с помощью значения заинкременченного счетчика,
 * а счетчик этот обновляется в DOM новым значением.
 */
trait MultiUpload extends AjaxFileUpload {

  /** Постпроцессинг параметров ajax-запроса для аплоада: добавить поддержку индексированных множественных значений. */
  override protected def _fileUploadAjaxArgsPp(ajaxSettings0: JQueryAjaxSettings, input: HTMLInputElement,
                                               cont: JQuery): JQueryAjaxSettings = {

    val ajaxSettings1 = super._fileUploadAjaxArgsPp(ajaxSettings0, input, cont)

    // Счетчик индексов имен полей (последнее использованное значение).
    val counterOpt: Option[Int] = {
      val raw = input.getAttribute(UploadConstants.ATTR_MULTI_INDEX_COUNTER)
      Option(raw)
        .filter(!_.isEmpty)
        .map(_.toInt)
    }

    // Выставлен счетчик -- значит включен режим мульти-аплоада. Для этого и написан весь текущий модуль.
    if (counterOpt.isDefined) {
      val index0 = counterOpt.get
      val index2 = index0 + 1
      // Обновить ссылку в аргументах.
      ajaxSettings1.url = {
        val url0 = ajaxSettings1.url
        val sep = if (url0.indexOf('?') >= 0) "&" else "?"
        url0 + sep + UploadConstants.NAME_INDEX_QS_NAME + "=" + index2
      }
      // Сохранить новое значение счетчика в DOM. Лучше это делать в фоне, т.к. это может быть потребовать времени.
      Future {
        input.setAttribute(UploadConstants.ATTR_MULTI_INDEX_COUNTER, index2.toString)
      }
    }

    // Вернуть подправленный экземпляр аргументов ajax-запроса.
    ajaxSettings1
  }

}
