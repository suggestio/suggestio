package models.adv.build

import io.suggest.es.model.ITryUpdateData
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.util.effect.RWT
import io.suggest.model.n2.node.MNode
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future

/** Аккамулятор результатов асинхронного билдера размещения.
  *
  * @param mnode Обрабатываемый узел, например рекламная карточка.
  * @param dbActions Аккамулятор DBIO Actions, которые необходимо будет в ходе транзакции накатить.
  */
case class Acc(
                mnode             : MNode,
                dbActions         : List[DBIOAction[_, NoStream, RWT]]  = Nil,
                ctxOuterFut       : Future[MCtxOuter]                   = MCtxOuter.emptyFut,
                interruptedTypes  : Set[MItemType]                      = Set.empty
              ) {

  def withMnode(mnode: MNode) = copy(mnode = mnode)
  def withDbActions(dbActions: List[DBIOAction[_, NoStream, RWT]]) = copy(dbActions = dbActions)
  def withInterruptedTypes(interruptedTypes: Set[MItemType]) = copy(interruptedTypes = interruptedTypes)

}


/** Обёртка для [[Acc]] для передачи в EsModelUtil.tryUpdate(). */
final case class TryUpdateBuilder(acc: Acc) extends ITryUpdateData[MNode, TryUpdateBuilder] {

  override def _saveable = acc.mnode

  override def _instance(m: MNode) = {
    val acc2 = acc.withMnode(m)
    TryUpdateBuilder( acc2 )
  }

}
