package io.suggest.util

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import Lists._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 19:41
 * Description:
 */
class ListsTest extends FlatSpec with ShouldMatchers {

  "insideOut()" should "do the work" in {
    insideOut(Map(1 -> List('a','b','c'), 2 -> List('b', 'c', 'd'))) should equal (Map('a' -> Set(1), 'b' -> Set(1,2), 'c' -> Set(1,2), 'd' -> Set(2)))
  }

  "mergeMaps()" should "merge" in {
    mergeMaps(Map(1 -> 'a', 2 -> 'b', 3 -> 'c'), Map(2 -> 'd')) { (_, _, v2) => v2 } should equal (Map(1 -> 'a', 2 -> 'd', 3 -> 'c'))
    mergeMaps(
      Map("asd" -> Set(1,2,3), "bsd" -> Set(3,4,5)),
      Map("asd" -> Set(2,3,4), "bsd" -> Set(1,2))
    ) {(_, s1, s2) => s1 ++ s2} should equal (Map("asd" -> Set(1,2,3,4), "bsd" -> Set(1,2,3,4,5)))
  }

}
