package io.suggest.model.geo

import io.suggest.util.JacksonWrapper
import org.scalatest._
import play.api.libs.json.Json
import java.{util => ju}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:23
 * Description: Тесты для Linestring GeoShape'ов.
 */
class LineStringGsTest extends MultiPoingGeoShapeTest {

  override type T = LineStringGs
  override def companion = LineStringGs

  override protected def JSON_EXAMPLE: String = {
    """
      |{
      |  "type" : "linestring",
      |  "coordinates" : [[-77.03653, 38.897676], [-77.009051, 38.889939]]
      |}
    """.stripMargin
  }

  override protected def JSON_EXAMPLE_PARSED: LineStringGs = {
    LineStringGs(Seq(
      GeoPoint(lat = 38.897676, lon = -77.03653),
      GeoPoint(lat = 38.889939, lon = -77.009051)
    ))
  }

}


/** Общий код multipoint-фигур лежит здесь. */
trait MultiPoingGeoShapeTest extends FlatSpec with Matchers with CoordLineRnd {

  protected val testsPerTry = 100
  override val minCoordLineLen = 2
  override val coordLineLenRnd = 40
  
  type T <: GeoShapeQuerable

  def companion: MultiPointShapeStatic { type Shape_t = T }

  protected def mkTests(f: T => Unit): Unit = {
    (0 to testsPerTry) foreach { i =>
      val coords = rndCoordRow
      f(companion.apply(coords))
    }
  }


  "Jackson JSON" should "serialize/deserialize to/from ES JSON" in {
    mkTests { lsgs =>
      val jsonStr = Json.stringify( lsgs.toPlayJson() )
      val jacksonJson = JacksonWrapper.deserialize [ju.HashMap[Any, Any]] (jsonStr)
      companion.deserialize(jacksonJson)  shouldBe  Some(lsgs)
      lsgs.toEsShapeBuilder  should not be  null
    }
  }

  protected def JSON_EXAMPLE: String
  protected def JSON_EXAMPLE_PARSED: T

  "play.json DATA_FORMAT" should "parse documented example" in {
    val jsr = Json.parse(JSON_EXAMPLE)
      .validate(companion.DATA_FORMAT)
    assert( jsr.isSuccess, jsr )
    val mpgs = jsr.get
    mpgs shouldBe JSON_EXAMPLE_PARSED
  }

}

