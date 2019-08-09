package io.suggest.geo

import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.{Json, OFormat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:23
 * Description: Тесты для Linestring GeoShape'ов.
 */
class LineStringGsSpec extends MultiPoingGeoShapeTest {

  override type T = LineStringGs

  override implicit def jsonFormat: OFormat[LineStringGs] =
    IGeoShape.JsonFormats.allStoragesEsFormatter.lineString

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
      MGeoPoint(lat = 38.897676, lon = -77.03653),
      MGeoPoint(lat = 38.889939, lon = -77.009051)
    ))
  }

}


/** Общий код multipoint-фигур лежит здесь. */
trait MultiPoingGeoShapeTest extends FlatSpec with CoordLineRnd {

  override val minCoordLineLen = 2
  override val coordLineLenRnd = 40
  
  type T <: IGeoShapeQuerable

  //def companion: MultiPointShapeStatic { type Shape_t = T }
  implicit def jsonFormat: OFormat[T]


  protected def JSON_EXAMPLE: String
  protected def JSON_EXAMPLE_PARSED: T

  "play.json DATA_FORMAT" should "parse documented example" in {
    val jsr = Json.parse(JSON_EXAMPLE)
      .validate[T]
    assert( jsr.isSuccess, jsr )
    val mpgs = jsr.get
    mpgs shouldBe JSON_EXAMPLE_PARSED
  }

}

