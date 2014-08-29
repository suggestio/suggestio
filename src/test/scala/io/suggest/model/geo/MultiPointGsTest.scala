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
}
