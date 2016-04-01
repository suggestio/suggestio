package models.adv.build

import models._
import org.elasticsearch.client.Client
import slick.dbio.{DBIOAction, Effect, NoStream}

import scala.concurrent.{Future, ExecutionContext}

/** Аккамулятор результатов асинхронного билдера размещения.
  *
  * @param mad Карточка рекламная.
  * @param dbActions Аккамулятор DBIO Actions, которые необходимо будет в ходе транзакции накатить.
  */
case class Acc(
  mad         : MNode,
  dbActions   : List[DBIOAction[_, NoStream, Effect.Write]] = Nil,
  ctxOuterFut : Future[MCtxOuter] = MCtxOuter.emptyFut
)


/** Обёртнка для [[Acc]] для передачи в EsModelUtil.tryUpdate(). */
case class TryUpdateBuilder(acc: Acc)
                           (implicit override val ec: ExecutionContext,
                            implicit override val client: Client)
    extends MNode.TryUpdateDataAbstract[TryUpdateBuilder]
{
  override def _saveable = acc.mad

  override protected def _instance(m: MNode) = TryUpdateBuilder( acc.copy(mad = m) )
}
