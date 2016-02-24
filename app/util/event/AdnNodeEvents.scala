package util.event

import akka.actor.ActorContext
import com.google.inject.Inject
import io.suggest.event.SNStaticSubscriber
import io.suggest.event.SioNotifier.{Classifier, Event, Subscriber}
import io.suggest.event.subscriber.SnClassSubscriber
import io.suggest.model.n2.node.event.MNodeSaved
import models.event.{MEvent, MEventType, MEventTypes}
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.15 11:36
 * Description: При создании узла надо добавить в него кое-какие начальные события.
 * Для этого нужно отреагировать на событие создание узла.
 *
 */
class AdnNodeEvents @Inject() (
  mCommonDi             : ICommonDi
)
  extends SNStaticSubscriber
  with SnClassSubscriber
  with PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Автодобавление уведомления о создании нового магазина можно отключить через конфиг. */
  private val EVT_YOU_CAN_ADD_NEW_SHOPS = configuration.getBoolean("node.evn.created.youCanAddNewShopsEvent")
    .getOrElse(true)

  /** Автодобавление уведомления о возможности использования менеджера рекламных карточек. */
  private val EVT_START_YOUR_WORK_USING_CARD_MGR = configuration.getBoolean("node.evt.created.startYourWorkUsingCardMgr")
    .getOrElse(true)


  /** Карта подписки на события [[SiowebNotifier]]. */
  override def snMap: List[(Classifier, Seq[Subscriber])] = {
    val subs = Seq(this)
    val someTrue = Some(true)
    List(
      // TODO 2016.feb.24: Удалено связывание MEvent и биллинга. MEvent надо сначала переписать к MNode,
      //      и только потом к новому биллингу привязывать уже.
      //AdvSavedEvent.getClassifier(isCreated = someTrue)     -> subs,
      MNodeSaved.getClassifier(isCreated = someTrue)   -> subs
    )
  }

  /** Добавляем приветствие "вы можете добавлять новые магазины"... */
  def addEvtAddNewShops(adnId: String) = addEvt(adnId, MEventTypes.YouCanCreateNewShop)

  /** Добавляем узлу событие, что можно попользоваться карточками. */
  def addEvtUseCardMgr(adnId: String) = addEvt(adnId, MEventTypes.StartYourWorkUsingCardMgr)

  /** Создать и сохранить event указанного типа для указанного узла. */
  private def addEvt(adnId: String, etype: MEventType): Future[String] = {
    val evt = MEvent(
      etype       = etype,
      ownerId     = adnId,
      isCloseable = true,
      isUnseen    = true
    )
    val fut = evt.save
    fut.onFailure {
      case ex =>
        error("Failed to save welcome event: " + evt, ex)
    }
    fut
  }


  /**
   * При создании узла нужно добавить в MEvent несколько приветственных событий.
   * @param event событие создания узла.
   * @param ctx контекст sio-notifier.
   */
  override def publish(event: Event)(implicit ctx: ActorContext): Unit = {
    event match {
      // Произошел insert узла.
      case anse: MNodeSaved =>
        import anse.nodeId
        if (EVT_YOU_CAN_ADD_NEW_SHOPS) {
          addEvtAddNewShops(nodeId)
        }
        if (EVT_START_YOUR_WORK_USING_CARD_MGR) {
          addEvtUseCardMgr(nodeId)
        }

      // Произошло insert одного из вариантов adv
      //case ase: AdvSavedEvent =>
      //  addEvtForAdv(ase.adv)

      // Should never happen...
      case other =>
        warn("Unexpected msg received: " + other)
    }
  }

}
