package io.suggest.scalaz

import minitest._

import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.10.17 21:09
  * Description: Тесты для [[ZTreeUtil]].
  */
object ZTreeUtilSpec extends SimpleTestSuite {

  private val tree1: Tree[String] = {
    Tree.Node(
      "root1",
      Stream(
        Tree.Leaf( "leaf11" ),
        Tree.Leaf( "leaf12" ),
        Tree.Node(
          "node13",
          Stream(
            Tree.Leaf("leaf131"),
            Tree.Leaf("leaf132"),
            Tree.Leaf("leaf133")
          )
        ),
        Tree.Leaf( "leaf14" ),
        Tree.Node(
          "node15",
          Stream(
            Tree.Leaf("leaf151")
          )
        ),
        Tree.Leaf( "leaf16" )
      )
    )
  }


  import ZTreeUtil._


  test("Generate valid level-0 tree path") {
    assertEquals(
      tree1.nodeToPath("root1"),
      Some(Nil)
    )
  }

  test("Handle Nil tree path") {
    assertEquals(
      tree1
        .loc
        .pathToNode(Nil)
        .toLabelOpt,
      Some("root1")
    )
  }


  test("Generate path for level 1: leaf12") {
    assertEquals(
      tree1.nodeToPath( "leaf12" ),
      Some(List(1))
    )
  }

  test("Handle path for level 1: leaf12") {
    assertEquals(
      tree1
        .loc
        .pathToNode(List(1))
        .toLabelOpt,
      Some("leaf12")
    )
  }


  test("Generate path for level2: leaf132") {
    assertEquals(
      tree1.nodeToPath( "leaf132" ),
      Some(List(2, 1))
    )
  }


  test("Handle path for level2: leaf132") {
    assertEquals(
      tree1
        .loc
        .pathToNode( List(2,1) )
        .toLabelOpt,
      Some("leaf132")
    )
  }

}
