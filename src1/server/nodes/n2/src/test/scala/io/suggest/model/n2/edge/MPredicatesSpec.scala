package io.suggest.model.n2.edge

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 11:28
 * Description: Тесты для enum-модели [[MPredicates]].
 */
class MPredicatesSpec extends AnyFlatSpec with PlayJsonTestUtil {

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
  it should "properly support Receiver << Receiver.Self" in {
    assert(Receiver.Self hasParent Receiver, Receiver.Self)
  }


  "eqOrHasParent(p)" should "self" in {
    val p = OwnedBy
    assert( p.eqOrHasParent(p), p )
  }
  it should "Receiver <<== Receiver.Self" in {
    assert(Receiver.Self eqOrHasParent Receiver, Receiver.Self)
  }
  it should "Receiver.Self <<== Receiver.Self" in {
    val p = Receiver.Self
    assert(p eqOrHasParent p, p)
  }


  "parentsIterator()" should "return empty iter for Receiver" in {
    val pi = Receiver.parentsIterator
    assert( pi.isEmpty, pi )
  }
  it should "return non-empty iter for Receiver.Self" in {
    val pi = Receiver.Self.parentsIterator
    assert( pi.nonEmpty, pi )
    assert( pi.next == Receiver, pi )
    assert( pi.isEmpty, pi )
  }


  "meAndParentsIterator()" should "return Receiver.Self followed by Receiver" in {
    val mp = Receiver.Self.meAndParentsIterator.toList
    assert( mp.size == 2, mp )
    assert( mp.head == Receiver.Self, mp)
    assert( mp.tail == List(Receiver), mp)
  }

}
