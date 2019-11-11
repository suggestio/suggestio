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
object NodesTreeWalkSpec extends NodesTreeWalkerSpecT with NodesTreeUpdateSpecT {


  protected case class Node(
                             id       : String,
                             children : Seq[Node] = Nil
                           )
    extends IId[String]

  override type Node_t = Node

  protected object Nodes extends NodesTreeApiIId with NodesTreeWalk with NodeTreeUpdate {
    override type T = Node
    override def _subNodesOf(node: Node) = node.children
    override def withNodeChildren(node: Node, children2: IterableOnce[Node]): Node = {
      node.copy(children = children2.toSeq)
    }
  }

  override type Nodes_t = Nodes.type
  override val Nodes_t = Nodes

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


/** Базовый трейт для сборки разных тестов разных трейтов TreeNodes*. */
trait NodesTreeSpecBase extends SimpleTestSuite {

  /** Класс модели. */
  type Node_t <: IId[String]

  type Nodes_t <: NodesTreeApiIId { type T = Node_t }

  protected def Nodes_t: Nodes_t

  protected def m1: Node_t

}


/** Тесты для дерева, которые можно пошарить между спеками. */
trait NodesTreeWalkerSpecT extends NodesTreeSpecBase {

  override type Nodes_t <: NodesTreeApiIId with NodesTreeWalk { type T = Node_t }

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


/** Тесты для update-функций. */
trait NodesTreeUpdateSpecT extends NodesTreeSpecBase {

  override type Nodes_t <: NodesTreeApiIId with NodeTreeUpdate { type T = Node_t }


  // Тесты вокруг узла вернего уровня.

  test("flatMapNode() can delete top node / will produce empty tree") {
    val nodeId = "asdadasdsa"
    val r = Nodes_t
      .flatMapNode(nodeId :: Nil, m1)( Nodes_t.deleteF )
      .toSeq
    assert( r.isEmpty, r.toString )
  }

  test("flatMapNode() deletion for missing node will produce unmodified tree") {
    val nodeId = "WHOAAA_INEXISTING_ID"
    val r = Nodes_t
      .flatMapNode(nodeId :: Nil, m1)( Nodes_t.deleteF )
      .toSeq
    assertEquals( r, Seq(m1) )
  }

  test("flatMapNode() update for top-node will return new tree") {
    val nodeId = "asdadasdsa"
    val r = Nodes_t
      .flatMapNode(nodeId :: Nil, m1) { node0 =>
        val node2 = Nodes_t.withNodeChildren(node0, Nil)
        node2 :: Nil
      }
      .toSeq
    val m1_2 = Nodes_t.withNodeChildren(m1, Nil)
    assertEquals( r, Seq(m1_2) )
  }


  // Убогонькие минимальные тесты на втором уровне:
  test("flatMapNode() update one node on non-1-st level") {
    val rcvrKey = "asdadasdsa" :: "sub2-asdadasdsa" :: "sub2233-asdadasdsa" :: Nil
    val updatedNode = Nodes_t.withNodeChildren(m1, Nil)
    val r = Nodes_t
      .flatMapNode(rcvrKey, m1) { _ =>
        updatedNode :: Nil
      }
      .toSeq
    assert( r != Seq(m1), r.toString )

    val m1_2 = r.head
    // Проверить, что первый узел остался тот же
    val topNodeNoChildren = Nodes_t.withNodeChildren( m1_2, Nil )
    assertEquals( updatedNode, topNodeNoChildren )

    // Проверить, что узел rcvrKey больше не существует (т.к. он был замененён другим узлом с другим id).
    val r1 = Nodes_t
      .flatMapNode(rcvrKey, m1_2) { _ =>
        throw new IllegalArgumentException("Should be never called here")
      }
      .toSeq
    assertEquals(r, r1)
  }

}
