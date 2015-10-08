package io.suggest

import play.api.libs.json.{Json, Format}
import org.scalatest.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 23:58
 * Description: Тестирование play json сериализации.
 */
trait PlayJsonTesting {

  protected def _jsonTest[T](e: T)(implicit format: Format[T]): Unit = {
    val jsv = Json.toJson(e)
    val jsr = jsv.validate[T]
    assert(jsr.isSuccess, jsr)
    jsr.get shouldBe e
  }

}
