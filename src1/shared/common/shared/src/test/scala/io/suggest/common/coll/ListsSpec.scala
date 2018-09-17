package io.suggest.common.coll

import minitest._
import io.suggest.common.coll.Lists._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 19:41
 * Description: Тесты для Lists
 */
object ListsSpec extends SimpleTestSuite {

  test("insideOut()") {
    assertEquals(
      insideOut(Map(1 -> List('a','b','c'), 2 -> List('b', 'c', 'd'))),
      Map('a' -> Set(1), 'b' -> Set(1,2), 'c' -> Set(1,2), 'd' -> Set(2))
    )
  }

  test("mergeMaps()") {
    assertEquals(
      mergeMaps(Map(1 -> 'a', 2 -> 'b', 3 -> 'c'), Map(2 -> 'd')) { (_, _, v2) => v2 },
      Map(1 -> 'a', 2 -> 'd', 3 -> 'c')
    )

    assertEquals(
      mergeMaps(
        Map("asd" -> Set(1,2,3), "bsd" -> Set(3,4,5)),
        Map("asd" -> Set(2,3,4), "bsd" -> Set(1,2))
      ) {(_, s1, s2) => s1 ++ s2},
      Map("asd" -> Set(1,2,3,4), "bsd" -> Set(1,2,3,4,5))
    )
  }


  test("findLCS() should find longest common sub-seq, not ragged") {
    assertEquals(
      findLCS(List(1,2,3,4,5,6,7,8,9), List(7,8,9,10)) ,
      List(7,8,9)
    )
    assertEquals(
      findLCS(List(1,2,3,4,5,6,7,8,9), List(1,4,7,8,9,10)) ,
      List(7,8,9)
    )
    assertEquals(
      findLCS(List(1,2,3,4,5,6,7,8,9), List(1)),
      Nil
    )
    assertEquals(
      findLCS(List(1,2,3,4,5,6,7,8,9), Nil) ,
      Nil
    )
    assertEquals( findLCS(Nil, Nil), Nil )
    assertEquals(
      findLCS(List(1,2,3,4,5,6,7,8,9), List(1,2,3, 5,6,7,8,9, 10)) ,
      List(5,6,7,8,9)
    )
    assertEquals(
      findLCS(List(1,2,3,4,5,6,7,8,9), List(1,2, 4,5,6,7,8, 10)) ,
      List(4,5,6,7,8)
    )
    assertEquals(
      findLCS(
        List(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17),
        List(1,2,3,4,10,11,12,13,14,15,16,17)
      ),
      List(10,11,12,13,14,15,16,17)
    )
  }


  test("findRaggedLCS() should find longest common ragged sub-sequence") {
    assertEquals(
      findRaggedLCS(Array("a", "b", "c"), Array("b", "c", "d")) ,
      List("b", "c")
    )
    assertEquals(
      findRaggedLCS(Array("a", "b", "c"), Array("e", "d", "f")) ,
      Nil
    )
    assertEquals(
      findRaggedLCS(Array(1, 2, 3, 4, 5), Array(2, 4, 6)) ,
      List(2, 4)
    )
    assertEquals(
      findRaggedLCS(
        Array(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17),
        Array(1,2,3,4, 10,11,12,13,14,15,16,17)
      ),
      List(1,2,3,4, 10,11,12,13,14,15,16,17)
    )
  }


  test("isElemsEqs() for two empty lists") {
    assert( isElemsEqs[String](List.empty, Stream.empty) )
  }
  test("isElemsEqs() should true for same lists") {
    val l1 = List("1", "2", "b", "ft")
    val l2 = l1.toStream
    assert( isElemsEqs(l1, l2) )
  }
  test("isElemsEq() for slightly different lists with same length") {
    val l1 = List("1", "2", "b", "ft")
    val l2 = "x" :: l1.tail
    assertEquals(
      isElemsEqs(l1, l2),
      false
    )
  }
  test("isElemsEq() for diff-len lists") {
    val l1 = List("1", "2", "b", "ft")
    val l2 = l1 ++ List("uhh")
    assertEquals(
      isElemsEqs(l1, l2),
      false
    )
  }

}
