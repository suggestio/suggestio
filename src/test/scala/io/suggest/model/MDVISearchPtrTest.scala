package io.suggest.model

import org.scalatest._
import MDVISearchPtr._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.13 15:58
 * Description:
 */
class MDVISearchPtrTest extends FlatSpec with Matchers {

  // Проверить сериализацию/десериализацию idOpt в название колонки.
  "idOpt" should "be serializeable/deserializeable as column qualifier" in {
    idOpt2column(None) should equal(COLUMN_PREFIX.getBytes)
    column2idOpt(idOpt2column(None)) should equal (None)
    column2idOpt(idOpt2column(Some("ddd"))) should equal (Some("ddd"))
    column2idOpt(idOpt2column(Some("asd.asd123"))) should equal (Some("asd.asd123"))
  }

  // Сериализация/десериализация vins
  "vins" should "be serializeable/deserializeable as column qualifier" in {
    val v0 = List("asd")
    serializeVins(v0) should equal (v0.head.getBytes)
    deserializeVins(serializeVins(v0)) should equal (v0)

    val v1 = List("asd", "fff", "rrr")
    deserializeVins(serializeVins(v1)) should equal (v1)

    deserializeVins(serializeVins(Nil)) should equal (Nil)
  }

}
