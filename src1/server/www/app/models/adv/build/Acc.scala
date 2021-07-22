package models.adv.build

import io.suggest.es.model.ITryUpdateData
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.util.effect.RWT
import io.suggest.n2.node.MNode
import monocle.macros.GenLens
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
                ctxOuterFut       : Future[MCtxOuter],
                interruptedTypes  : Set[MItemType]                      = Set.empty
              )

object Acc {
  def mnode = GenLens[Acc](_.mnode)
  def dbActions = GenLens[Acc](_.dbActions)
  def interruptedTypes = GenLens[Acc](_.interruptedTypes)
}


/** Обёртка для [[Acc]] для передачи в EsModelUtil.tryUpdate(). */
final case class TryUpdateBuilder(acc: Acc) extends ITryUpdateData[MNode, TryUpdateBuilder] {

  override def _saveable = acc.mnode

  override def _instance(m: MNode) = {
    val acc2 = (Acc.mnode set m)(acc)
    TryUpdateBuilder( acc2 )
  }

}
