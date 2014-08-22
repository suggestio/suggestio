package io.suggest.model.geo

import org.scalatest._
import org.elasticsearch.common.unit.DistanceUnit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 14:31
 * Description: Тесты для модели Distance, описывающей неизменяемое расстояние.
 */
class DistanceTest extends FlatSpec with Matchers {

  /** Значения дистанций для тестов без указания единиц. */
  private val vs: Seq[Double] = Seq(12.0, 1.0, 100.0, 0.244, 10.5, 123.0, 3545.0, 56.56)

  /** Запускалка тестов для всех значений и единиц измерения. */
  private def mkTest(f: Distance => Unit): Unit = {
    vs.foreach { dv =>
      DistanceUnit.values().foreach { du =>
        val d = Distance(dv, du)
        f(d)
      }
    }
  }


  "Distance model" should "serialize/deserialize to/from JSON via inst -> String-> inst" in {
    mkTest { d =>
      Distance.parseDistance( d.toString ) shouldBe d
    }
  }

  it should "serialize/deserialize to/from es.Distance class" in {
    mkTest { d =>
      Distance.parseDistance( d.toEsDistance ) shouldBe d
    }
  }

}
