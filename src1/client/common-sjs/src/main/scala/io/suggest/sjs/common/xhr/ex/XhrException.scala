package io.suggest.sjs.common.xhr.ex

import io.suggest.common.html.HtmlConstants.SPACE
import org.scalajs.dom.XMLHttpRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:27
 * Description: Исключения при исполнении XHR-запроса (без jquery или иных помошников).
 * Если включена фильтрация по http-статусу ответа сервера, то будет этот экзепшен при недопустимом статусе.
 */
case class XhrFailedException(
                               xhr: XMLHttpRequest,
                               url: String = null,
                               method: String = null,
                               override val getCause: Exception = null
                             )
  extends RuntimeException {

  override def getMessage: String = {
    val urlStr = Option(url)
      .orElse(xhr.responseURL.toOption)
      .getOrElse("")
    val methodStr = Option(method).getOrElse("")

    methodStr + SPACE +
    urlStr + SPACE +
    xhr.status + SPACE +
    xhr.readyState + SPACE
  }

  override final def toString = getMessage

}

