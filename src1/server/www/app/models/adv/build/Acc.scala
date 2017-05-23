package models.adv.build

import io.suggest.mbill2.util.effect.RWT
import io.suggest.model.n2.node.IMNodes
import models._
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future

/** Аккамулятор результатов асинхронного билдера размещения.
  *
  * @param mad Карточка рекламная.
  * @param dbActions Аккамулятор DBIO Actions, которые необходимо будет в ходе транзакции накатить.
  */
case class Acc(
                mad         : MNode,
                dbActions   : List[DBIOAction[_, NoStream, RWT]] = Nil,
                ctxOuterFut : Future[MCtxOuter] = MCtxOuter.emptyFut
)


/** Трейт для поддержки TryUpdateBuilder'а под нужды Adv-подсистемы.
  * Велосипед на костылях, надо будет этот ужас переписать. */
trait AdvMNodesTryUpdateBuilderT extends IMNodes {

  /** Обёртнка для [[Acc]] для передачи в EsModelUtil.tryUpdate(). */
  case class TryUpdateBuilder(acc: Acc)
    extends mNodes.TryUpdateDataAbstract[TryUpdateBuilder] {
    override def _saveable = acc.mad

    override protected def _instance(m: MNode) = TryUpdateBuilder(acc.copy(mad = m))
  }

}