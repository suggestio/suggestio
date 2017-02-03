package io.suggest.geo

import org.elasticsearch.common.unit.DistanceUnit
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json.{JsString, Json}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 14:31
 * Description: Тесты для модели Distance, описывающей неизменяемое расстояние.
 */
class DistanceTest extends FlatSpec {

  /** Значения дистанций для тестов без указания единиц. */
  private val vs: Seq[Double] = Seq(12.0, 1.0, 100.0, 0.244, 10.5, 123.0, 3545.0, 56.56)

  /** Запускалка тестов для всех значений и единиц измерения. */
  private def mkTest[T](f: Distance => T): Unit = {
    vs.foreach { dv =>
      DistanceUnit.values().foreach { du =>
        val d = Distance(dv, du)
        f(d)
      }
    }
  }


  "Legacy jackson JSON" should "serialize/deserialize to/from JSON via inst -> String-> inst" in {
    mkTest { d =>
      Distance.parseDistance( d.toString ) shouldBe d
    }
  }

  it should "serialize/deserialize to/from es.Distance class" in {
    mkTest { d =>
      Distance.parseDistance( d.toEsDistance ) shouldBe d
    }
  }


  "play.json" should "handle model" in {
    mkTest { d =>
      val jsv = Json.toJson( d )
      assert( jsv.isInstanceOf[JsString], jsv )
      val res = jsv.validate[Distance]
      assert( res.isSuccess, res )
      val d2 = res.get
      d2 shouldBe d
    }
  }

}
