package io.suggest.sjs.common.xhr

import io.suggest.common.html.HtmlConstants.SPACE

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:27
 * Description: Исключения при исполнении XHR-запроса (без jquery или иных помошников).
 * Если включена фильтрация по http-статусу ответа сервера, то будет этот экзепшен при недопустимом статусе.
 */
case class HttpFailedException(
                               resp: Option[HttpResp] = None,
                               url: String = null,
                               method: String = null,
                               override val getCause: Throwable = null
                             )
  extends RuntimeException {

  override def getMessage: String = {
    val urlStr = Option(url)
      .getOrElse("")
    val methodStr = Option(method).getOrElse("")

    methodStr + SPACE +
    urlStr + SPACE +
    resp.fold("")(_.status + SPACE)
  }

  override final def toString = getMessage

}
