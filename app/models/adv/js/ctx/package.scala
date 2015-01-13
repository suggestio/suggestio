package models.adv.js

import play.api.libs.json.JsObject

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.01.15 17:03
 */
package object ctx {

  /** Тип контекста, которым обменивается сервер и js-подсистема. */
  type JsCtx_t    = JsObject

  type PictureUploadMode = PictureUploadModes.PictureUploadMode

}
