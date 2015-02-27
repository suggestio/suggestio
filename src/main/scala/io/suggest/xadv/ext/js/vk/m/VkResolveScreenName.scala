package io.suggest.xadv.ext.js.vk.m

import scala.scalajs.js

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


case class VkResolveScreenNameArgs(screenName: String) {
  /** Сериализация. В таком виде можно передавать в API. */
  def toJson = js.Dictionary[js.Any](
    SCREEN_NAME_FN -> screenName
  )
}



/** Результат вызовы resolveScreenName(). */
case class VkResolveScreenNameResult(vkType: String, vkId: Long)

