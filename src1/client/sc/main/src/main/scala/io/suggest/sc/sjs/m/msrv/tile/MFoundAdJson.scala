package io.suggest.sc.sjs.m.msrv.tile

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import io.suggest.sc.ScConstants.Resp.HTML_FN

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.05.15 18:20
  * Description: JSON-интерфейс для ответа сервера по одной отрендеренной карточке для плитки.
  */

@js.native
sealed trait MFoundAdJson extends js.Object {

  @JSName( HTML_FN )
  var html: String = js.native

}
