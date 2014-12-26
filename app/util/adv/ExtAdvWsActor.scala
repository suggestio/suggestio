package util.adv

import _root_.util.{PlayLazyMacroLogsImpl, PlayMacroLogsI}
import _root_.util.ws.{SubscribeToWsDispatcher, WsActorDummy}
import akka.actor.{Actor, ActorRef, Props}
import io.suggest.model.EsModel.FieldsJsonAcc
import models.JsRawCode
import models.adv.MExtAdvQs
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 16:24
 * Description: Утиль и код актора, который занимается общением с js api размещения рекламных карточек на клиенте.
 */
object ExtAdvWsActor {

  def props(out: ActorRef, args: MExtAdvQs) = Props(new ExtAdvWsActor(out, args))

}

/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor(out: ActorRef, args: MExtAdvQs)
  extends WsActorDummy
  with SubscribeToWsDispatcher
  with ExtAdvWsActorCore
  with PlayLazyMacroLogsImpl
{
  override def wsId: String = args.wsId
}


/** Ядро актора, взаимодействующего с данными в браузере.
  * Внутри это FSM, который хранит в себе кое-какое состояние по сделанным операциям и по грядущим действиям. */
sealed trait ExtAdvWsActorCore extends Actor with PlayMacroLogsI {

  def out: ActorRef
  def args: MExtAdvQs

  def sioPrJs = "SioPR"

  // TODO Нужны состояния, обработка переключение и вся остальная логика.
  override def preStart(): Unit = {
    super.preStart()
    // Сразу отправить команду инициализации js-api:
    out ! prepareEnsureReadyJson
    // TODO Выставить состояние ожидания ответа инициализации.
  }

  /** Начальный обработчик сообщений. */
  abstract override def receive: Receive = super.receive orElse {
    case v: JsValue =>
      LOGGER.error("TODO")
  }


  private def onSuccessJson(name: String, args: FieldsJsonAcc): JsObject = {
    JsObject(Seq(
      "replyTo" -> JsString(name),
      "status"  -> JsString("success"),
      "args"    -> JsObject(args)
    ))
  }
  
  private def onErrorJson(name: String, reason: String, args: FieldsJsonAcc = Nil): JsObject = {
    JsObject(Seq(
      "replyTo" -> JsString(name),
      "status"  -> JsString("error"),
      "args"    -> JsObject(
        "reason" -> JsRawCode(reason) ::
        args
      )
    ))
  }

  /** Генерация js-кода запуска приветствия клиенского сервера. */
  private def prepareEnsureReadyJs: String = {
    val action = "prepareEnsureReady"
    val successJson = onSuccessJson(action, List(
      "ctx1" -> JsRawCode("ctx1")
    ))
    // TODO всякие начальные данные в состояние залить бы...
    val ctx0 = JsObject(Nil)
    val errorJson = onErrorJson(action, reason = "reason")
    new StringBuilder(256, sioPrJs)
      .append('.')
      .append(action).append('(').append(ctx0).append(')')
      .append(".execute(")
      .append("function(ws, ctx1) { ws.send(").append(successJson).append(");},")
      .append("function(ws, reason) { ws.send(").append(errorJson).append(");});")
      .toString()
  }

  /** Генерация JSON'а приветствия клиентского сервера. */
  private def prepareEnsureReadyJson: JsObject = {
    JsObject(Seq(
      "type" -> JsString("js"),
      "data" -> JsString(prepareEnsureReadyJs)
    ))
  }

}
