package io.suggest.xadv.ext.js.vk.c

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 13:42
 * Description:
 */
package object low {

  type JSON = js.Dictionary[js.Dynamic]

  type Callback = js.Function1[JSON, _]

}
