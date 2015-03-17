package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.03.15 11:58
 * Description: Модель аргументов и результата вызова vk api метода account.getAppPermissions().
 */
case class VkGetAppPermissionsArgs(
  userId: Option[Int] = None
) extends IToJsonDict {

  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any]()
    if (userId.isDefined)
      d.update("user_id", userId.get)
    d
  }

}


object VkGetAppPermissionsResult extends FromJsonT {
  override type T = VkGetAppPermissionsResult

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]: WrappedDictionary[Any]
    apply(
      bitMask = d("response").asInstanceOf[Int]
    )
  }
}

case class VkGetAppPermissionsResult(
  bitMask: Int
)
