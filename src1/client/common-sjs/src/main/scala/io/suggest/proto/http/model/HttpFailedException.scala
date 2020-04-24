package io.suggest.proto.http.model

import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.text.StringUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:27
 * Description: Исключения при исполнении XHR-запроса (без jquery или иных помошников).
 * Если включена фильтрация по http-статусу ответа сервера, то будет этот экзепшен при недопустимом статусе.
 */
case class HttpFailedException(
                               resp   : Option[HttpResp] = None,
                               url    : String = null,
                               method : String = null,
                               override val getCause: Throwable = null,
                               errMessage: String = null,
                             )
  extends RuntimeException
{

  override def getMessage: String = {
    if (errMessage != null) {
      errMessage
    } else {
      val urlStr = Option( url )
        .fold("") { url1 =>
          StringUtil.strLimitLen(url1, 35)
        }

      val methodStr = Option(method) getOrElse ""

      resp.fold("") { r =>
        r.status.toString + SPACE
        // r.body - это Future(), поэтому надо заносить его в errMessage через конструктор.
      } + Option(getCause).fold("") { ex2 =>
        ex2.getClass.getSimpleName + SPACE + ex2.getMessage + SPACE
      } + methodStr +
        SPACE + urlStr
    }
  }

  override final def toString = getMessage

}
