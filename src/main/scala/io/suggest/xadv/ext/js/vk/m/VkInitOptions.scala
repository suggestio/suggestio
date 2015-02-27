package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.IToJsonDict

import scala.scalajs.js
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 16:39
 * Description: Опции инициализации клиента vk.
 */
case class VkInitOptions(appId: String) extends IToJsonDict {
  override def toJson = js.Dictionary[js.Any](
    "apiId" -> appId
  )
}

