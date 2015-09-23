package io.suggest.model.n2.edge

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.common.menum.play._
import io.suggest.model.n2.node.{MNodeTypes, MNodeType}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 10:32
 * Description: Статическая синхронная модель предикатов, т.е. "типов" ребер графа N2.
 * Создана по мотивам модели zotonic m_predicate.
 */
object MPredicates extends EnumMaybeWithName with EnumJsonReadsT {

  /** Класс одного элемента модели. */
  protected[this] abstract sealed class Val(val strId: String)
    extends super.Val(strId)
  {

    /** Типы узлов, которые могут выступать субъектом данного предиката. */
    def fromTypes  : List[MNodeType]

    /** Типы узлов, которые могут выступать объектами данного предиката. */
    def toTypes   : List[MNodeType]

  }

  override type T = Val


  // Экземпляры модели, идентификаторы идут по алфавиту: a->z, a1->z1, ...

  /** Карточка-объект произведена узлом-субъектом. */
  val AdOwnedBy: T = new Val("a") {

    override def fromTypes: List[MNodeType] = {
      List(MNodeTypes.AdnNode)
    }

    override def toTypes: List[MNodeType] = {
      List(MNodeTypes.Ad)
    }

  }


}
