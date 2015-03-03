package util.adv

import akka.actor.Props
import io.suggest.util.UrlUtil
import models.adv._
import models.adv.js._
import models.adv.js.ctx.MJsCtx
import models.event.{MEventTmp, RenderArgs}
import play.api.libs.json.JsString
import util.PlayMacroLogsImpl
import util.async.FsmActor
import util.event.EventTypes
import util.jsa.JsAppendById
import ExtUtil.RUNNER_EVENTS_DIV_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.02.15 14:41
 * Description: Актор, занимающийся обработкой на уровне сервисов.
 * Появился, когда возникла необходимость изолированной инициализации в рамках одного сервиса.
 * Когда инициализация сервиса завершена, супервизор получает список целей, готовых к дальнейшей обработке.
 */

object ExtServiceActor {

  def props(args: IExtAdvServiceActorArgs): Props = {
    Props(ExtServiceActor(args))
  }

}

case class ExtServiceActor(args: IExtAdvServiceActorArgs)
  extends FsmActor
  with PlayMacroLogsImpl
  with MediatorSendCommand
{ actor =>

  import args.ctx   // Нужно для рендера шаблонов событий
  import LOGGER._

  override protected var _state: FsmState = new DummyState

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive = allStatesReceiver

  protected def replyTo = self.path.name

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureServiceState)
  }

  /** Запуск инициализации клиента для одного сервиса. */
  class EnsureServiceState extends FsmState {

    /** При переходе на это состояние надо отправить запрос на инициализацию в рамках таргета. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Отправить запрос на подготовку к работе.
      val mctx0 = MJsCtx(
        action = Some(MJsActions.EnsureReady),
        domain = args.targets
          .iterator
          .map { tg => UrlUtil.url2dkey(tg.target.url) }
          .toSet
          .toSeq,
        service = Some(args.service)
      )
      val cmd = EnsureReadyAsk(mctx0, replyTo = Some(replyTo))
      sendCommand(cmd)
    }

    /** Ожидаем получения результата инициализации. */
    override def receiverPart: Receive = {
      // Супервизор прислал распарсенный ws-ответ от js по текущему сервису.
      case ans: Answer if ans.ctx2.status.nonEmpty =>
        ans.ctx2.status.get match {
          // Клиент успешно завершил инициализацию. Нужно собрать акторов и отправить их ws-медиатору.
          case AnswerStatuses.Success =>
            trace("Successful answer received from client, preparing tg-actors...")
            val tgActors = args.targets.map { tg =>
              val actorArgs = new IExtAdvTargetActorArgs with IExtAdvArgsWrapperT {
                override def mctx0              = ans.ctx2
                override def target             = tg
                override def _eaArgsUnderlying  = args
                override def wsMediatorRef      = args.wsMediatorRef
              }
              ExtTargetActor.props(actorArgs)
            }
            args.wsMediatorRef ! AddActors(tgActors)

          // Не удалось инициализировать клиента для связи с сервисом. Отрендерить плашку события юзеру.
          case _ =>
            warn(s"Failed to initialize service ${args.service} client-side: ${ans.ctx2.error}")
            val mevent = MEventTmp(
              etype       = EventTypes.AdvServiceError,
              ownerId     = args.request.producerId,
              isCloseable = false,
              isUnseen    = true
            )
            val rargs = RenderArgs(
              mevent        = mevent,
              withContainer = false,
              adnNodeOpt    = Some(args.request.producer),
              advExtTgs     = args.targets.map(_.target),
              madOpt        = Some(args.request.mad),
              extServiceOpt = Some(args.service)
            )
            val html = rargs.mevent.etype.render(rargs)
            val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
            val jsa = JsAppendById(RUNNER_EVENTS_DIV_ID, htmlStr)
            val cmd = JsCmd(
              jsCode = jsa.renderToString()
            )
            sendCommand(cmd)
        }
        // Этот актор больше не нужен при любом раскладе.
        context.stop(self)
    }
  }

}
