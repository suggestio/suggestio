package io.suggest.model.n2.edge

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 11:28
 * Description: Тесты для enum-модели [[MPredicates]].
 */
class MPredicatesSpec extends FlatSpec with PlayJsonTestUtil {

  import MPredicates._

  override type T = MPredicate


  "JSON" should "support all model's elements" in {
    for (e <- values) {
      jsonTest(e)
    }
  }


  "hasParent(p)" should "!self" in {
    val p = OwnedBy
    assert( !p.hasParent(p), p )
  }
  it should "properly support GeoParent << Direct" in {
    assert(GeoParent.Direct hasParent GeoParent, GeoParent.Direct)
  }


  "eqOrHasParent(p)" should "self" in {
    val p = OwnedBy
    assert( p.eqOrHasParent(p), p )
  }
  it should "GeoParent <<== Direct" in {
    assert(GeoParent.Direct eqOrHasParent GeoParent, GeoParent.Direct)
  }
  it should "GeoParent <<== GeoParent" in {
    val p = GeoParent.Direct
    assert(p eqOrHasParent p, p)
  }


  "parentsIterator()" should "return empty iter for GeoParent" in {
    val pi = GeoParent.parentsIterator
    assert( pi.isEmpty, pi )
  }
  it should "return non-empty iter for GeoParent.Direct" in {
    val pi = GeoParent.Direct.parentsIterator
    assert( pi.nonEmpty, pi )
    assert( pi.next == GeoParent, pi )
    assert( pi.isEmpty, pi )
  }


  "meAndParentsIterator()" should "return GeoParent.Direct followed by GeoParent" in {
    val mp = GeoParent.Direct.meAndParentsIterator.toList
    assert( mp.size == 2, mp )
    assert( mp.head == GeoParent.Direct, mp)
    assert( mp.tail == List(GeoParent), mp)
  }

}
