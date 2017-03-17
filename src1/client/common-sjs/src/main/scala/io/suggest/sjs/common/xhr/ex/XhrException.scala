package io.suggest.sjs.common.xhr.ex

import org.scalajs.dom.{ErrorEvent, Event, XMLHttpRequest}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 11:27
 * Description: Исключения при исполнении XHR-запроса (без jquery или иных помошников).
 */

sealed trait XhrException extends RuntimeException {
  def xhr: XMLHttpRequest
}


/** Если включена фильтрация по http-статусу ответа сервера, то будет этот экзепшен при недопустимом статусе. */
case class XhrUnexpectedRespStatusException(xhr: XMLHttpRequest) extends XhrException {

  override def getMessage: String = {
    "Unexpected XHR resp status: " + xhr.status
  }

}

