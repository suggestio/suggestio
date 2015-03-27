package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.IToJsonDict

import scala.scalajs.js
import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 18:04
 * Description: Модель параметров инициализации fb js клиента.
 */

object FbInitOptions {

  /**
   * Используемая версия Fb api.
   * @see [[https://developers.facebook.com/docs/apps/changelog]]
   */
  def API_VSN = "v2.3"

}


import FbInitOptions._


/**
 * Экземпляр модели.
 * @param appId id аппликухи.
 */
case class FbInitOptions(
  appId   : String
) extends IToJsonDict {

  override def toJson: Dictionary[Any] = {
    js.Dictionary[js.Any](
      "appId"   -> appId,
      "xfbml"   -> true,
      "cookie"  -> true,
      "version" -> API_VSN
    )
  }
}
