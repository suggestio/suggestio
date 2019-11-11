package io.suggest.stat.m

import _root_.play.api.libs.json._
import io.suggest.primo.TypeT
import org.scalatest.matchers.should.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:54
 * Description: Быстрое тестирования сериализации в JSON с последующей десериализацией (play.json).
 */
trait PlayJsonTestUtil extends TypeT {

  protected[this] def jsonTest(v: T)(implicit format: Format[T]): Unit = {
    val jsv = Json.toJson(v)
    val jsr = jsv.validate[T]
    assert( jsr.isSuccess, (jsv, jsr) )
    jsr.get shouldBe v
  }

}
