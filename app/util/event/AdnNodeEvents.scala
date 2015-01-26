package util.event

import akka.actor.ActorContext
import io.suggest.event.{AdnNodeSavedEvent, SNStaticSubscriber}
import io.suggest.event.SioNotifier.{Event, Subscriber, Classifier}
import io.suggest.event.subscriber.SnClassSubscriber
import models.MAdvReq
import models.adv.AdvSavedEvent
import models.event.MEvent
import util.PlayLazyMacroLogsImpl
import play.api.Play.{current, configuration}
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.15 11:36
 * Description: При создании узла надо добавить в него кое-какие начальные события.
 * Для этого нужно отреагировать на событие создание узла.
 */
object AdnNodeEvents extends SNStaticSubscriber with SnClassSubscriber with PlayLazyMacroLogsImpl {

  /** Автодобавление уведомления о создании нового магазина можно отключить через конфиг. */
  val EVT_YOU_CAN_ADD_NEW_SHOPS = configuration.getBoolean("node.evn.created.youCanAddNewShopsEvent")
    .getOrElse(true)

  /** Автодобавление уведомления о возможности использования менеджера рекламных карточек. */
  val EVT_START_YOUR_WORK_USING_CARD_MGR = configuration.getBoolean("node.evt.created.startYourWorkUsingCardMgr")
    .getOrElse(true)


  /** Карта подписки на события [[SiowebNotifier]]. */
  override def snMap: List[(Classifier, Seq[Subscriber])] = {
    val subs = Seq(this)
    val someTrue = Some(true)
    List(
      AdnNodeSavedEvent.getClassifier(isCreated = someTrue)   -> subs,
      AdvSavedEvent.getClassifier(isCreated = someTrue)       -> subs
    )
  }

  /** Добавляем приветствие "вы можете добавлять новые магазины"... */
  def addEvtAddNewShops(adnId: String) = addEvt(adnId, EventTypes.YouCanCreateNewShop)

  /** Добавляем узлу событие, что можно попользоваться карточками. */
  def addEvtUseCardMgr(adnId: String) = addEvt(adnId, EventTypes.StartYourWorkUsingCardMgr)

  /** Создать и сохранить event указанного типа для указанного узла. */
  private def addEvt(adnId: String, etype: EventType): Future[String] = {
    val evt = MEvent(
      etype       = etype,
      ownerId     = adnId,
      isCloseable = true,
      isUnseen    = true
    )
    val fut = evt.save
    fut.onFailure {
      case ex =>
        LOGGER.error("Failed to save welcome event: " + evt, ex)
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
      case anse: AdnNodeSavedEvent =>
        import anse.adnId
        if (EVT_YOU_CAN_ADD_NEW_SHOPS) {
          addEvtAddNewShops(adnId)
        }
        if (EVT_START_YOUR_WORK_USING_CARD_MGR) {
          addEvtUseCardMgr(adnId)
        }

      // Произошло insert одного из вариантов adv
      case ase: AdvSavedEvent if false =>   // TODO удалить if false, когда всё будет готово.
        ase.adv match {
          case req: MAdvReq =>
            ???
        }

      case other =>
        LOGGER.warn("Unexpected msg received: " + other)
    }
  }
}
