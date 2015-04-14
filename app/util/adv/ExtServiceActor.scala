package util.adv

import io.suggest.util.UrlUtil
import models.adv._
import models.adv.ext.act.ExtServiceActorEnv
import models.adv.js._
import util.PlayMacroLogsImpl
import util.async.FsmActor
import ut._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.02.15 14:41
 * Description: Актор, занимающийся обработкой на уровне сервисов.
 * Появился, когда возникла необходимость изолированной инициализации в рамках одного сервиса.
 * Когда инициализация сервиса завершена, супервизор получает список целей, готовых к дальнейшей обработке.
 */

object ExtServiceActor extends IServiceActorCompanion


/** Очень базовая логика service-актора. Вынесена из актора, чтобы была возможность заюзать эту логику
  * ещё в каком-нибудь акторе. */
trait ExtServiceActorLogic extends FsmActor with ISendCommand with ExtServiceActorEnv with SvcActorJsRenderUtil {

  /** Абстрактный state инициализации сервиса. */
  trait EnsureServiceStateStub extends FsmState {

    /** При переходе на это состояние надо отправить запрос на инициализацию в рамках таргета. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Отправить запрос на подготовку к работе.
      val mctx0 = args.mctx0.copy(
        action = Some(MJsActions.EnsureReady),
        domain = args.targets
          .iterator
          .map { tg => UrlUtil.url2dkey(tg.target.url) }
          .toSet
          .toSeq,
        service = Some(service),
        // Заливаем в контекст все имеющиеся цели. js обдумает их и вернет список возможно в каком-то модифицированном виде.
        svcTargets = args.targets.map(tg2jsTg)
      )
      val cmd = EnsureReadyAsk(mctx0, replyTo = Some(replyTo))
      sendCommand(cmd)
    }

    /** Обработка успешного ответа. */
    def handleSuccessAnswer(ans: Answer): Unit

    /** Обработка ответа с ошибкой. */
    def handleInvalidAnswer(ans: Answer): Unit

    def handleInitFinished(): Unit

    /** Ожидаем получения результата инициализации. */
    override def receiverPart: Receive = {
      // Супервизор прислал распарсенный ws-ответ от js по текущему сервису.
      case ans: Answer if ans.ctx2.status.nonEmpty =>
        ans.ctx2.status.get match {
          // Клиент успешно завершил инициализацию. Нужно собрать акторов и отправить их ws-медиатору.
          case AnswerStatuses.Success =>
            LOGGER.trace("Successful answer received from client, preparing tg-actors...")
            handleSuccessAnswer(ans)

          // Не удалось инициализировать клиента для связи с сервисом. Отрендерить плашку события юзеру.
          case _ =>
            LOGGER.warn(s"Failed to initialize service $service client-side: ${ans.ctx2.error}")
            handleInvalidAnswer(ans)
        }
        handleInitFinished()
    }
  }

}


case class ExtServiceActor(args: IExtAdvServiceActorArgs)
  extends ExtServiceActorLogic
  with ExtServiceActorEnv
  with ReplyTo
  with PlayMacroLogsImpl
  with MediatorSendCommand
{ actor =>

  import LOGGER._

  override protected var _state: FsmState = new DummyState

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive = allStatesReceiver

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureServiceState)
  }


  /** Запуск инициализации клиента для одного сервиса. */
  class EnsureServiceState extends EnsureServiceStateStub {

    /** Превращение списка таргетов к списку таргетов. */
    lazy val tgtsMap: Map[String, MExtTargetInfoFull] = {
      args.targets
        .iterator
        .flatMap { tgFull =>
          tgFull.target.id
            .iterator
            .map { _ -> tgFull }
        }
        .toMap
    }

    override def afterBecome(): Unit = {
      super.afterBecome()
      // запускаем исполнение lazy val tgtsMap, когда сообщение уже отправлено.
      tgtsMap
    }

    /** Обработка успешного ответа. */
    override def handleSuccessAnswer(ans: Answer): Unit = {
      val tMap = tgtsMap
      // Отмаппить возвращенный список таргетов на карту исходных, отправить акторов на исполнение.
      val tgActors = ans.ctx2.svcTargets flatMap { jsTg =>
        tMap.get(jsTg.id) match {
          case Some(mtg) =>
            val mctx3 = ans.ctx2.copy(
              svcTargets = Nil,
              target = Some(jsTg)
            )
            val actorArgs = new IExtAdvTargetActorArgs with IExtAdvArgsWrapperT {
              override def mctx0 = mctx3
              override def target = mtg
              override def _eaArgsUnderlying = args
              override def wsMediatorRef = args.wsMediatorRef
            }
            val props = ExtTargetActor.props(actorArgs)
            Seq(props)

          // Возможна невероятная ситуация, когда пришел неправильный id и это дропнуто. Нужно сообщить об этом в логи и юзеру.
          case None =>
            warn(s"Js returned target_id[${jsTg.id}], but this target not found. Target info is: $jsTg")
            Nil
        }
      }
      args.wsMediatorRef ! AddActors(tgActors)
    }

    /** Обработка ответа с ошибкой. */
    override def handleInvalidAnswer(ans: Answer): Unit = {
      serviceInitFailedRender(
        errors = ans.ctx2.error.toSeq
      )
    }

    override def handleInitFinished(): Unit = {
      // Этот актор больше не нужен при любом раскладе.
      context.stop(self)
    }
  }

}
