package io.suggest.sjs.common.model.ex

import org.scalajs.dom.raw.XMLHttpRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 17:19
 * Description: XML Http request failed.
 */
case class XhrFailedException(
  xhr: XMLHttpRequest,
  errorThrown: String
) extends RuntimeException {

  override def getMessage: String = {
    "XHR failed with HTTP " + xhr.status + " " + xhr.statusText + ": " + errorThrown + ";; " + xhr.responseText
  }

}
