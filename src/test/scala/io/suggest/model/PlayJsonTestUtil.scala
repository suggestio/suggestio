package io.suggest.model

import io.suggest.primo.TypeT
import org.scalatest.Matchers._
import play.api.libs.json.{Json, OFormat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:54
 * Description: Быстрое тестирования сериализации в JSON с последующей десериализацией (play.json).
 */
trait PlayJsonTestUtil extends TypeT {

  protected[this] def jsonTest(v: T)(implicit format: OFormat[T]): Unit = {
    val jsv = Json.toJson(v)
    jsv.as[T] shouldBe v
  }

}
