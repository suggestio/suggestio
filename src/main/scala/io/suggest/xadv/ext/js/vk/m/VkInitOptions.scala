package io.suggest.xadv.ext.js.vk.m

import scala.scalajs.js
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 16:39
 * Description:
 */
case class VkInitOptions(appId: String)

object VkInitOptions {

  implicit def opts2json(opts: VkInitOptions): js.Dynamic = {
    js.Dynamic.literal(
      apiId = opts.appId
    )
  }

}
