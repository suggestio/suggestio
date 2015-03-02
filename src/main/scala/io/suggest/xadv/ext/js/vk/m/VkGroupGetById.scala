package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.{IToJsonDict, FromJsonT}

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 17:21
 * Description: Параметры и результат вызова vk API groups.getById().
 */
object VkGroupGetByIdArgs {

  def GROUP_ID_FN = "group_id"
  def FIELDS_FN   = "fields"

  def CAN_POST_FN = "can_post"

  def apply(groupId: UserId_t, fields: String): VkGroupGetByIdArgs = {
    apply(groupId.toString, fields)
  }
}


import VkGroupGetByIdArgs._


case class VkGroupGetByIdArgs(groupId: String, fields: String) extends IToJsonDict {
  def toJson = js.Dictionary[js.Any](
    GROUP_ID_FN -> groupId,
    FIELDS_FN   -> fields
  )
}


object VkGroupGetByIdResult extends FromJsonT {
  override type T = VkGroupGetByIdResult

  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Any]]: WrappedDictionary[js.Any]
    val resp = d.get("response").flatMap { resp =>
      val arr = resp.asInstanceOf[js.Array[js.Dictionary[js.Any]]]
      arr.headOption
    }
    VkGroupGetByIdResult(
      name = resp
        .flatMap(_.get("name"))
        .fold("")(_.toString),
      canPost = resp
        .flatMap(_.get(CAN_POST_FN))
        .map { v => v.asInstanceOf[Int] > 0 },
      deactivated = resp
        .flatMap(_.get("deactivated"))
        .map(_.toString)
    )
  }
}

/** Распарсенный результат. */
case class VkGroupGetByIdResult(
  name          : String,
  canPost       : Option[Boolean],
  deactivated   : Option[String]
)
