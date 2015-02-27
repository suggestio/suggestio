package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.{IToJsonDict, FromJsonT}

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 15:39
 * Description: Модель аргументов вызова API utils.resolveScreenName().
 */
object VkResolveScreenNameArgs {

  /** Название поля JSON-аргументов, которое хранит имя, подлежащее резолву в vk id. */
  def SCREEN_NAME_FN = "screen_name"

}


import VkResolveScreenNameArgs._


case class VkResolveScreenNameArgs(screenName: String) extends IToJsonDict {
  /** Сериализация. В таком виде можно передавать в API. */
  override def toJson = js.Dictionary[js.Any](
    SCREEN_NAME_FN -> screenName
  )
}



object VkResolveScreenNameResult extends FromJsonT {

  override type T = VkResolveScreenNameResult

  /** Десериализация ответа API. */
  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Any]] : WrappedDictionary[js.Any]
    VkResolveScreenNameResult(
      vkType  = d("type").toString,
      vkId    = d("object_id").asInstanceOf[Long]
    )
  }

}

/** Результат вызовы resolveScreenName(). */
case class VkResolveScreenNameResult(vkType: String, vkId: Long)

