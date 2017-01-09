package io.suggest.model.geo

import io.suggest.geo.MGeoPoint

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:18
 * Description: Тесты для multipoint geo shape. Они совпадают с LineString-тестами.
 */
class MultiPointGsTest extends MultiPoingGeoShapeTest {

  override type T = MultiPointGs
  override def companion = MultiPointGs

  override protected def JSON_EXAMPLE: String = {
    """
      |{
      |  "type" : "multipoint",
      |  "coordinates" : [
      |      [102.0, 2.0], [103.0, 2.0]
      |  ]
      |}
    """.stripMargin
  }

  override protected def JSON_EXAMPLE_PARSED: MultiPointGs = {
    MultiPointGs(Seq(
      MGeoPoint(lat = 2.0, lon = 102.0),
      MGeoPoint(lat = 2.0, lon = 103.0)
    ))
  }
}
