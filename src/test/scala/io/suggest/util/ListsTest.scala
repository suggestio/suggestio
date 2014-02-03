package io.suggest.util

import org.scalatest._
import Lists._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 19:41
 * Description:
 */
class ListsTest extends FlatSpec with Matchers {

  "insideOut()" should "do the work" in {
    insideOut(Map(1 -> List('a','b','c'), 2 -> List('b', 'c', 'd'))) shouldEqual Map('a' -> Set(1), 'b' -> Set(1,2), 'c' -> Set(1,2), 'd' -> Set(2))
  }

  "mergeMaps()" should "merge" in {
    mergeMaps(Map(1 -> 'a', 2 -> 'b', 3 -> 'c'), Map(2 -> 'd')) { (_, _, v2) => v2 } shouldEqual Map(1 -> 'a', 2 -> 'd', 3 -> 'c')
    mergeMaps(
      Map("asd" -> Set(1,2,3), "bsd" -> Set(3,4,5)),
      Map("asd" -> Set(2,3,4), "bsd" -> Set(1,2))
    ) {(_, s1, s2) => s1 ++ s2} should equal (Map("asd" -> Set(1,2,3,4), "bsd" -> Set(1,2,3,4,5)))
  }


  "findLCS()" should "find longest common sub-seq, not ragged" in {
    findLCS(List(1,2,3,4,5,6,7,8,9), List(7,8,9,10))              shouldEqual List(7,8,9)
    findLCS(List(1,2,3,4,5,6,7,8,9), List(1,4,7,8,9,10))          shouldEqual List(7,8,9)
    findLCS(List(1,2,3,4,5,6,7,8,9), List(1))                     shouldEqual Nil
    findLCS(List(1,2,3,4,5,6,7,8,9), Nil)                         shouldEqual Nil
    findLCS(Nil, Nil)                                             shouldEqual Nil
    findLCS(List(1,2,3,4,5,6,7,8,9), List(1,2,3, 5,6,7,8,9, 10))  shouldEqual List(5,6,7,8,9)
    findLCS(List(1,2,3,4,5,6,7,8,9), List(1,2, 4,5,6,7,8, 10))    shouldEqual List(4,5,6,7,8)
    findLCS(
      List(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17),
      List(1,2,3,4,10,11,12,13,14,15,16,17)
    ) shouldEqual List(10,11,12,13,14,15,16,17)
  }


  "findRaggedLCS()" should "find longest common ragged sub-sequence" in {
    findRaggedLCS(Array("a", "b", "c"), Array("b", "c", "d"))   shouldEqual List("b", "c")
    findRaggedLCS(Array("a", "b", "c"), Array("e", "d", "f"))   shouldEqual Nil
    findRaggedLCS(Array(1, 2, 3, 4, 5), Array(2, 4, 6))         shouldEqual List(2, 4)
    findRaggedLCS(
      Array(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17),
      Array(1,2,3,4, 10,11,12,13,14,15,16,17)
    ) shouldEqual List(1,2,3,4, 10,11,12,13,14,15,16,17)
  }
}
