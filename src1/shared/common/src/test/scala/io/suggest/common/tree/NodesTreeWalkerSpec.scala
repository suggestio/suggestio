package io.suggest.common.tree

import io.suggest.primo.id.IId
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.17 10:06
  * Description: Тесты для модельной утили, работающей с деревьями узлов/графов.
  * Начальные тесты были сформированы ранее в [[io.suggest.adv.rcvr.MRcvrPopupNodeSpec]].
  */
object NodesTreeWalkerSpec extends NodesTreeWalkerSpecT {

  // Имитируем какую-то абстрактную модель узлов:

  protected case class Node(
                             id       : String,
                             children : Seq[Node] = Nil
                           )
    extends IId[String]

  override type Node_t = Node

  override val Nodes_t = new NodesTreeWalkerIId[Node] {
    override protected def _subNodesOf(node: Node) = node.children
  }

  /** Пример верхнего узла дерева узлов. */
  override val m1 = Node(
    id = "asdadasdsa",

    children = Seq(
      Node(
        id = "sub1-asdadasdsa",

        children = Seq(
          Node(
            id = "sub1-asdadasdsa"
          )
        )
      ),

      Node(
        id = "sub2-asdadasdsa",

        children = Seq(
          Node(
            id = "sub22-asdadasdsa"
          ),

          Node(
            id = "sub2233-asdadasdsa"
          )
        )
      ),

      Node(
        id = "sub3-asdadasdsa",

        children =  Seq(
          Node(
            id = "sub33-asdadasdsa"
          )
        )
      )
    )
  )

}


/** Тесты для дерева, которые можно пошарить между спеками. */
trait NodesTreeWalkerSpecT extends SimpleTestSuite {

  /** Класс модели. */
  type Node_t <: IId[String]

  protected def Nodes_t: NodesTreeWalkerIId[Node_t]

  protected def m1: Node_t


  test("findNode() for missing top-level node") {
    val r = Nodes_t.findNode(
      rcvrKey = "WHOOOAAA_SOMETHING_INVALID_ID" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for missing sub-node") {
    val r = Nodes_t.findNode(
      rcvrKey = "asdadasdsa" :: "WHOOOAAA_SOMETHING_INVALID_ID" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for completely missing sub-sub-sub-node") {
    val r = Nodes_t.findNode(
      rcvrKey = "asdadasdsa" :: "WHOOOAAA_SOMETHING_INVALID_ID" :: "ZZZZZZZZZZZZzz" :: "" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for completely missing inner-level node") {
    val r = Nodes_t.findNode(
      rcvrKey = "ZZZZZZZZZ_INVALID_ID" :: "WHOOOAAA_SOMETHING_INVALID_ID" :: "" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for top-level node") {
    val nodeId = "asdadasdsa"
    val rOpt = Nodes_t.findNode(
      rcvrKey = nodeId :: Nil,
      node    = m1
    )
    assert(rOpt.nonEmpty, rOpt.toString)
    val r = rOpt.get

    assertEquals(r.id, nodeId)
  }

  test("findNode() for first existing sub-node") {
    val rcvrKey = "asdadasdsa" :: "sub1-asdadasdsa" :: Nil
    val rOpt = Nodes_t.findNode(
      rcvrKey = rcvrKey,
      node = m1
    )
    assert(rOpt.nonEmpty, rOpt.toString)
    val r = rOpt.get

    assertEquals(r.id, rcvrKey.last)
  }

  test("findNode() for some deep non-first node") {
    val rcvrKey = "asdadasdsa" :: "sub2-asdadasdsa" :: "sub2233-asdadasdsa" :: Nil
    val rOpt = Nodes_t.findNode(
      rcvrKey = rcvrKey,
      node = m1
    )
    assert(rOpt.nonEmpty, rOpt.toString)
    val r = rOpt.get

    assertEquals(r.id, rcvrKey.last)
  }

}
