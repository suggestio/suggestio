package models.mbill

import akka.actor.ActorContext
import com.google.inject.Inject
import io.suggest.event.SioNotifier.Event
import io.suggest.event.SNStaticSubscriber
import io.suggest.event.subscriber.SnClassSubscriber
import io.suggest.model.n2.node.event.MNodeDeleted
import play.api.db.Database
import util.PlayLazyMacroLogsImpl

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 12:03
 * Description: Подписчик на события удаления узла.
 * Он удаляет контракт, что вызывает каскадное удаление в bill-моделях.
 */
class DelContractsWhenNodeDeleted @Inject() (
  db              : Database,
  implicit val ec : ExecutionContext
)
  extends SNStaticSubscriber
  with SnClassSubscriber
  with PlayLazyMacroLogsImpl
{
  import LOGGER._

  /** Подписка на события. */
  override def snMap = List(
    MNodeDeleted.getClassifier() -> Seq(this)
  )

  /** Обработать наступившие событие. */
  override def publish(event: Event)(implicit ctx: ActorContext): Unit = {
    event match {
      case ande: MNodeDeleted =>
        Future {
          val totalDeleted = db.withConnection { implicit c =>
            MContract.deleteByAdnId(ande.nodeId)
          }
          info(s"Deleted $totalDeleted contracts for deleted adnId[${ande.nodeId}].")
        }

      case other =>
        warn("Unknown event received: " + other)
    }
  }

}
