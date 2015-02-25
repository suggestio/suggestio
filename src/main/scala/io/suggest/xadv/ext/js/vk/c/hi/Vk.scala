package io.suggest.xadv.ext.js.vk.c.hi

import io.suggest.xadv.ext.js.vk.c.low._
import io.suggest.xadv.ext.js.vk.m.VkLoginResult
import org.scalajs.dom

import scala.concurrent.{Future, Promise}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 10:07
 * Description: Hi-level надстройка над слишком низкоуровневым и примитивным API вконтакта.
 */
object Vk {
  def Api = VkApi
  def Auth = VkApiAuth
}


object VkApi {
}


/** Высокоуровневое API для аутентификации. Т.к. это API реализовано на уровне openapi.js, а не на http,
  * то scala-обертки реализуются намного проще. */
object VkApiAuth {

  /**
   * Пропедалировать залогиневание юзера.
   * @param accessLevel Уровень доступа.
   * @return Фьючерс с результатами логина.
   */
  def login(accessLevel: Int): Future[VkLoginResult] = {
    val p = Promise[VkLoginResult]()
    VkLow.Auth.login { res: JSON =>
      val msg = "not yet impl. login: " + res
      dom.console.error(msg)
      p failure new Exception(msg)
    }
    p.future
  }

}

