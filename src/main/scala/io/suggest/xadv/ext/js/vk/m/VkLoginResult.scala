package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}
import io.suggest.xadv.ext.js.vk.c.low.JSON

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 9:55
 * Description: Реакция vk api на запрос логина.
 *
 * Такой res возвращается при удачном логине:
 * res: Object
 *   status: "connected"
 *   session: Object
 *     expire: 1425027270
 *     mid: "345635645"
 *     secret: "oauth"
 *     sid: "0bf2eec87307342d301456463b10940be46bc96a3eb956d5c01684c498053c7379840c547e836833a7a34"
 *     sig: "2002633ba494db81dd98b13b97d93e36"
 *  user: Object
 *    domain: "id345635645"
 *    first_name: "Vasya"
 *    href: "https://vk.com/id345635645"
 *    id: "345635645"
 *    last_name: "Pupkin"
 *    nickname: ""
 */

object VkLoginResult extends FromJsonT {

  override type T = VkLoginResult

  def VK_ID_FN = "a"
  def NAME_FN  = "b"

  /**
   * Перегнать результат возврата vk в экземпляр модели.
   * @param raw JSON-выхлоп vk api
   * @return None если логин не удался. Some с инфой по текущему юзеру.
   */
  def maybeFromResp(raw: JSON): Option[T] = {
    val d = raw : WrappedDictionary[js.Any]
    d.get("session")
      .filter(v => v != null && !js.isUndefined(v))
      .map { sessionRaw =>
        // Юзер залогинен на сервисе.
        lazy val sessionDic = sessionRaw.asInstanceOf[js.Dictionary[Any]] : WrappedDictionary[Any]
        val userDic = d.get("user")
          .fold [WrappedDictionary[String]] (WrappedDictionary.empty) (_.asInstanceOf[js.Dictionary[String]])
        VkLoginResult(
          // TODO Небезопасно, т.к. от vk всегда приходит строка.
          vkId = userDic.get("id")
            .orElse { sessionDic.get("mid").map(_.toString) }
            .get.toInt,
          name = getName(userDic)
        )
      }
  }
  
  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[String]] : WrappedDictionary[String]
    VkLoginResult(
      vkId = d(VK_ID_FN).asInstanceOf[Int],
      name = d.get(NAME_FN)
    )
  }

  private def getStr(userDic: WrappedDictionary[String], fn: String): Option[String] = {
    userDic.get(fn)
      .map(_.trim)
      .filter(!_.isEmpty)
  }

  def getName(userDic: WrappedDictionary[String]): Option[String] = {
    val firstOpt = getStr(userDic, "first_name")
    val lastOpt  = getStr(userDic, "last_name")
    (firstOpt, lastOpt) match {
      case (Some(f), Some(l)) =>
        Some(f + " " + l)
      case _ =>
        firstOpt orElse lastOpt
    }
  }

}

import VkLoginResult._

case class VkLoginResult(vkId: UserId_t, name: Option[String]) extends IToJsonDict {
  override def toJson = {
    val d = js.Dictionary[js.Any](
      VK_ID_FN -> vkId
    )
    if (name.isDefined)
      d.update(NAME_FN, name.get)
    d
  }
}


