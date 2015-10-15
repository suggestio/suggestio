package io.suggest.model.geo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:18
 * Description: Тесты для multipoint geo shape. Они совпадают с LineString-тестами.
 */
class MultiPointGsTest extends MultiPoingGeoShapeTest {

  override type T = MultiPoingGs
  override def companion = MultiPoingGs

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

  override protected def JSON_EXAMPLE_PARSED: MultiPoingGs = {
    MultiPoingGs(Seq(
      GeoPoint(lat = 2.0, lon = 102.0),
      GeoPoint(lat = 2.0, lon = 103.0)
    ))
  }
}
