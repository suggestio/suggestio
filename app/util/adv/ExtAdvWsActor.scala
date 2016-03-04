package util.adv

import _root_.util.PlayMacroLogsImpl
import _root_.util.async.FsmActor
import _root_.util.jsa.JsAppendById
import _root_.util.ws.SubscribeToWsDispatcher
import akka.actor.{SupervisorStrategy, Actor, ActorRef, Props}
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import models.adv._
import models.adv.js.ctx.MJsCtx
import models.adv.js._
import models.event.{MEventTypes, RenderArgs, MEventTmp}
import models.mws.AnswerStatuses
import play.api.libs.json._
import ExtUtil.RUNNER_EVENTS_DIV_ID

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext
import scala.util.{Random, Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 16:24
 * Description: Утиль и код актора, который занимается общением с js api размещения рекламных карточек на клиенте.
 */
class ExtAdvWsActors @Inject() (
  factory: ExtAdvWsActorFactory
) {

  /** Сборка конфигурации актора. */
  def props(out: ActorRef, eactx: IExtWsActorArgs): Props = {
    Props( factory.create(out, eactx) )
  }

}


/** Интерфейс сборщика экземпляров ExtAdvWsActor. */
trait ExtAdvWsActorFactory {
  /** Сборка экземпляра класса актора. */
  def create(out: ActorRef, eactx: IExtWsActorArgs): ExtAdvWsActor
}


/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor @Inject() (
  @Assisted out     : ActorRef,
  @Assisted eactx   : IExtWsActorArgs,
  implicit val ec   : ExecutionContext
)
  extends FsmActor
  with SubscribeToWsDispatcher
  with PlayMacroLogsImpl
{ actor =>

  import LOGGER._

  override type State_t = FsmState

  /** Флаг, обозначающий, что на клиент отправлена ask-команда, и он занимается обработкой оной.
    * Если флаг true, то значит js-система занимается какими-то действиями, скорее всего юзеру какое-то окно
    * отображено. */
  protected var _jsAskLock: Boolean = false

  /** Очередь команд для клиента. */
  protected var _queue = Queue[IWsCmd]()

  val rnd = new Random()

  /** Сериализация и отправка одной js-команды в веб-сокет. */
  def sendJsCommand(jsCmd: IWsCmd): Unit = {
    out ! Json.toJson(jsCmd)
  }

  /** Получена js-команда от какого-то актора. Нужно в зависимости от ситуации отправить её в очередь
    * или же отправить немедлено, выставив флаг блокировки. */
  def enqueueCommand(ask: IWsCmd): Unit = {
    if (_jsAskLock) {
      // Клиент отрабатывает другой ask. Значит отправить в очередь.
      trace("Asking still locked. 1 command queued.")
      _queue = _queue.enqueue(ask)
    } else {
      trace("Client ready for asking. Sending command and locking...")
      // Клиент свободен. Считаем, что очередь пуста. Сразу отправляем, выставляя флаг.
      sendJsCommand(ask)
      _jsAskLock = true
    }
  }

  /** Необходимо пропедалировать очередь команд вперёд в связи с получением какого-то ответа от js. */
  def dequeueAnswerReceived(): Unit = {
    if (_queue.isEmpty) {
      // Очередь пуста. Снимает флаг блокировки.
      trace("Queue empty. Unlocking...")
      _jsAskLock = false
    } else {
      // В очереди ещё есть запросы к js -- отправляем следующий запрос.
      trace("Dequeuing & sending next command...")
      val (ask, newQueue) = _queue.dequeue
      sendJsCommand(ask)
      _queue = newQueue
      _jsAskLock = true
    }
  }

  /** Подписка на WsDispatcher требует указания wsId. */
  override def wsId: String = eactx.qs.wsId

  override def receive: Actor.Receive = allStatesReceiver

  /** Текущее состояние FSM хранится здесь. */
  override protected var _state: FsmState = new DummyState

  override def preStart(): Unit = {
    super.preStart()
    become(new JsInitState)
  }

  /** Придумываем рандомный идентификатор для нового актора. По этому id будут роутится js-ответы. */
  @tailrec final def guessChildName(): String = {
    val actorName = Math.abs(rnd.nextLong()).toString
    if ( context.child(actorName).isDefined )
      guessChildName()
    else
      actorName
  }

  /** Отрендерить и отправить на экран событие, описанное параметрами рендера. */
  protected def sendRenderEvent(rargs: RenderArgs): Unit = {
    import eactx.ctx
    val html = rargs.mevent.etype.render(rargs)
    val htmlStr = JsString(html.body)     // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = JsAppendById(RUNNER_EVENTS_DIV_ID, htmlStr)
    val cmd = JsCmd(
      jsCode = jsa.renderToString()
    )
    sendJsCommand(cmd)
  }


  /** Отправить запрос системе на инициализацию. */
  class JsInitState extends FsmState {
    /** Необходимо отправить запрос инициализации. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Отправить в ws запрос базовой инициализации системы.
      val mctx0 = MJsCtx(
        action = Some(MJsActions.Init)
      )
      val ask = InitAsk(mctx0)
      sendJsCommand(ask)
    }

    override def receiverPart: Receive = {
      case jso: JsObject =>
        val ans = jso.as[Answer]
        val nextState: FsmState = ans.ctx2.status match {
          // js подтвердил успешную инициализацию.
          case Some(AnswerStatuses.Success) =>
            trace("Global js init ok. Going to targets.")
            new WaitForTargetsState(ans.ctx2)

          // Нет сообщения об успехе. Значит, что-то не так (попапы блокируются например), и нужно уведомить юзера об этом.
          case _ =>
            error("Global js init failed: " + ans.ctx2.error)
            // отрендерить юзеру сообщение о проблеме.
            // TODO Пока поддерживается только отображение проблемы с блокировкой попапов. Нужно определять ошибку, и рендерить необходимый шаблон.
            val mevent = MEventTmp(
              etype       = MEventTypes.BrowserBlockPopupsError,
              ownerId     = eactx.request.producer.id.get,
              isCloseable = false,
              isUnseen    = true
            )
            val rargs = RenderArgs(
              mevent        = mevent,
              withContainer = false,
              adnNodeOpt    = Some(eactx.request.producer),
              advExtTgs     = Seq.empty,
              madOpt        = Some(eactx.request.mad),
              extServiceOpt = None,
              errors        = ans.ctx2.error.toSeq
            )
            sendRenderEvent(rargs)
            new DummyState
        }
        become(nextState)
    }
  }


  /** Состояние ожидания асинхронных данных по целям для размещения, запущенных в предыдущем состоянии. */
  class WaitForTargetsState(mctx0: MJsCtx) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить callback'и на фьючерс с таргетами.
      eactx.targetsFut onComplete {
        case Success(targets) => self ! TargetsReady(targets.toList)
        case Failure(ex)      => self ! TargetsFailed(ex)
      }
    }

    override def receiverPart: Receive = {
      case TargetsReady(targets) =>
        val tisCount = eactx.qs.targets.size
        val tgtsCount = targets.size
        if (tisCount != tgtsCount) {
          warn(s"Unexpected number of targets read from model: $tgtsCount, but $tisCount expected.\n  requested = ${eactx.qs.targets}  found = $targets")
        }
        if (targets.isEmpty) {
          throw new IllegalStateException("No targets found in storage, but it should.")
        } else {
          trace(s"$name waiting finished. Found ${targets.size} targets.")
          val mctx1 = mctx0.copy(status = None, action = None)
          // Сгруппировать таргеты по сервисам, запустить service-акторов, которые занимаются инициализацией клиентов сервисов.
          targets.groupBy(_.target.service).foreach {
            case (_service, _srvTgs) =>
              trace(s"Starting service ${_service} actor with ${_srvTgs.size} targets...")
              val actorArgs = new IExtAdvServiceActorArgs with IExtAdvArgsWrapperT {
                override def service            = _service
                override def targets            = _srvTgs
                override def _eaArgsUnderlying  = eactx
                override def wsMediatorRef      = self
                override def mctx0              = mctx1
              }
              val aprops = _service.extAdvServiceActor.props(actorArgs)
              context.actorOf(aprops, name = guessChildName())
            }
          become(new SuperviseServiceActorsState)
        }

      case TargetsFailed(ex) =>
        error(s"$name: Failed to aquire targers", ex)
        // TODO Рендерить ошибку на экран юзеру.
        become(new DummyState)
    }

    case class TargetsReady(targets: ActorTargets_t)
    case class TargetsFailed(ex: Throwable)
  }


  /** Состояние обработки двунаправленного обмена сообщениями. */
  class SuperviseServiceActorsState extends FsmState {
    override def receiverPart: Receive = {
      // Пришло сообщение из web-socket'а для указанного target-актора.
      // C @-биндингом и unapply() пока есть проблемы. Поэтому достаём JsObject, парсим его, и уже тогда делаем последующие действия.
      case jso: JsObject =>
        try {
          val ans = jso.as[Answer]
          dequeueAnswerReceived()
          if (ans.replyTo.nonEmpty) {
            val sel = context.system.actorSelection(self.path / ans.replyTo.get)
            trace(s"$name: Message received for slave service actor: ${ans.replyTo}...")
            // Пересылаем сообщение целевому актору.
            // Используем send() вместо forward(), чтобы скрыть out от подчинённых акторов.
            sel ! ans
          } else {
            allStatesReceiver(ans)
          }
        } catch {
          case ex: Exception =>
            warn("JSON received, but cannot parse answer:\n  " + jso, ex)
        }

      // Подчинённый актор хочет отправить js-код для исполнения на клиенте.
      case wsCmd: IWsCmd =>
        wsCmd.sendMode match {
          case CmdSendModes.Async =>
            sendJsCommand(wsCmd)
          case CmdSendModes.Queued =>
            enqueueCommand(wsCmd)
        }

      // Народ требует новых акторов.
      case AddActors(actors) =>
        debug(s"AddActors() with ${actors.size} actors from ${sender()}")
        actors.foreach { props =>
          context.actorOf(props, name = guessChildName())
        }
    }
  }

  /** Если не грохать повесившихся акторов, то будет бесконечный перезапуск, который не останавливается никак.
    * Как вариант, можно ограничить кол-во перезапусков актора до 2-3. */
  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

}


/** Трейт для service-компаниона. */
trait IServiceActorCompanion {
  def apply(args: IExtAdvServiceActorArgs): Actor

  def props(args: IExtAdvServiceActorArgs): Props = {
    Props(apply(args))
  }
}
