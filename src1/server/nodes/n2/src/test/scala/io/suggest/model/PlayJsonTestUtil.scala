package io.suggest.model

import io.suggest.primo.TypeT
import org.scalatest.matchers.should.Matchers._
import _root_.play.api.libs.json._
import io.suggest.event.{MockedSioNotifierStaticClient, ISioNotifierStaticClient}
import _root_.play.api.{Application, Mode}
import _root_.play.api.inject.bind
import _root_.play.api.inject.guice.GuiceApplicationBuilder
import io.suggest.es.util.{IEsClient, IEsClientMock}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

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


trait MockedEsSn { this: GuiceOneAppPerSuite =>

  override implicit lazy val app: Application = {
    new GuiceApplicationBuilder()
      .in( Mode.Test )
      .bindings(
        bind[IEsClient]
          .to[IEsClientMock],
        bind[ISioNotifierStaticClient]
          .to[MockedSioNotifierStaticClient],
      )
      .build()
  }

}
