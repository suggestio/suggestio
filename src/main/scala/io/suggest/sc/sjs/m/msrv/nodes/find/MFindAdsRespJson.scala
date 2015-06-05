package io.suggest.sc.sjs.m.msrv.nodes.find

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 16:24
 * Description: Интерфейс для доступа к полям сырого JSON-ответа сервера.
 */
sealed trait MFindAdsRespJson extends js.Object {

  /** HTML-верстка списка узлов. */
  var nodes: String = js.native

  /** timestamp генерации ответа. */
  var timestamp: Double = js.native

}

object MFindAdsRespJson {

  def apply(raw: js.Dynamic): MFindAdsRespJson = {
    raw.asInstanceOf[MFindAdsRespJson]
  }

}
