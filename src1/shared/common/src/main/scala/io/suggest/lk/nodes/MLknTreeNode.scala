package io.suggest.lk.nodes

import boopickle.Default._
import io.suggest.common.tree.{NodeTreeUpdate, NodesTreeApiIId, NodesTreeWalk}
import io.suggest.primo.id.IId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 14:57
  * Description: Кросс-платформенная модель данных по одному узлу в дереве узлов форме узлов.
  */

object ILknTreeNode
  extends NodesTreeApiIId
  with NodesTreeWalk
  with NodeTreeUpdate
{

  override type T = ILknTreeNode

  override protected def _subNodesOf(node: ILknTreeNode) = node.children

  override def withNodeChildren(node: ILknTreeNode, children2: TraversableOnce[ILknTreeNode]): ILknTreeNode = {
    node.withChildren(
      children2.toSeq
    )
  }

  /** Поддержка сериализации/десериализации. */
  implicit val lknTreeNodePickler: Pickler[ILknTreeNode] = {
    implicit val p = compositePickler[ILknTreeNode]
    p.addConcreteType[MLknTreeNode]
  }

}


/**
  * Интерфейс элемента дерева узлов.
  * Интерфейс необходим из-за рекурсивности pickler'а.
  */
sealed trait ILknTreeNode extends IId[String] {

  override val id     : String

  /** Отображаемое название узла. */
  val name            : String

  /** Код типа узла по модели MNodeTypes. */
  val ntypeId         : String

  /** Список дочерних узлов. */
  val children        : Seq[ILknTreeNode]

  /** Сборка инстанса с новым набором дочерних узлов. */
  def withChildren(children2: Seq[ILknTreeNode], loaded2: Boolean = true): ILknTreeNode

}


/**
  * Класс модели данных по узлу.
  * По идее, это единственая реализация [[ILknTreeNode]].
  */
case class MLknTreeNode(
                         override val id              : String,
                         override val name            : String,
                         override val ntypeId         : String,
                         override val children        : Seq[ILknTreeNode] = Nil
                       )
  extends ILknTreeNode
{

  override def withChildren(children2: Seq[ILknTreeNode], loaded2: Boolean): MLknTreeNode = {
    copy(
      children = children2
    )
  }

}
