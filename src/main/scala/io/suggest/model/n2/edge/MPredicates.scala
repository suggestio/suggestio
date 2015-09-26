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
object MPredicates extends EnumMaybeWithName with EnumJsonReadsValT {

  /** Класс одного элемента модели. */
  protected[this] abstract sealed class Val(val strId: String)
    extends super.Val(strId)
  {

    /** Типы узлов, которые могут выступать субъектом данного предиката. */
    def fromTypeValid(ntype: MNodeType): Boolean

    /** Типы узлов, которые могут выступать объектами данного предиката. */
    def toTypeValid(ntype: MNodeType): Boolean

  }

  override type T = Val


  // Короткие врапперы для определения принадлежности типов друг к другу.
  private def _isAdnNode(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.AdnNode
  }
  private def _isPerson(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.Person
  }
  private def _isAd(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.Ad
  }


  // Экземпляры модели, идентификаторы идут по алфавиту: a->z, a1->z1, ...

  /** Карточка-объект произведена узлом-субъектом. */
  val AdOwnedBy: T = new Val("a") {
    override def fromTypeValid(ntype: MNodeType): Boolean = {
      _isAdnNode(ntype)
    }
    override def toTypeValid(ntype: MNodeType): Boolean = {
      _isAd(ntype)
    }
  }


  /** Юзер владеет чем-то: узлом или карточкой напрямую. */
  val PersonOwns: T = new Val("b") {
    override def fromTypeValid(ntype: MNodeType): Boolean = {
      _isPerson(ntype)
    }
    override def toTypeValid(ntype: MNodeType): Boolean = {
      _isAdnNode(ntype) || _isAd(ntype)
    }
  }


  /**
   * Модерация запросов на размещение с from-узла делегирована другому указанному узлу.
   * from -- узел, входящие запросы на который будут обрабатываться узлом-делегатом
   * to   -- узел-делегат, который видит у себя запросы на размещение от других узлов.
   */
  val AdvManageDelegatedTo: T = new Val("c") {
    override def fromTypeValid(ntype: MNodeType): Boolean = {
      _isAdnNode(ntype)
    }
    override def toTypeValid(ntype: MNodeType): Boolean = {
      _isAdnNode(ntype)
    }
  }

  /**
   * Предикат юзера-создателя какого-то узла в системе.
   * from -- юзер.
   * to   -- любой узел, например карточка или магазин.
   */
  val CreatorOf: T = new Val("d") {
    override def fromTypeValid(ntype: MNodeType): Boolean = {
      _isPerson(ntype)
    }
    override def toTypeValid(ntype: MNodeType): Boolean = {
      true
    }
  }

}
