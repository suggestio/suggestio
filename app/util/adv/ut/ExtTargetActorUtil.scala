package util.adv.ut

import models.adv.IExtAdvTargetActorArgs
import models.adv.ext.act.ExtTargetActorEnv
import models.adv.js.JsCmd
import models.adv.js.ctx.JsErrorInfo
import models.event.{ErrorInfo, MEventTmp, RenderArgs}
import play.api.libs.json.JsString
import util.event._
import util.jsa.{InnerHtmlById, JsAppendById}
import util.adv.ExtUtil.RUNNER_EVENTS_DIV_ID

/** Утиль для сборки target-акторов. */
trait ExtTargetActorUtil extends ISendCommand with ReplyTo with ExtTargetActorEnv {

  override val args: IExtAdvTargetActorArgs

  import args.ctx

  def evtRenderArgs(etype: EventType, errors: ErrorInfo*): RenderArgs = {
    evtRenderArgs(etype, withContainer = false, errors : _*)
  }
  def evtRenderArgs(etype: EventType, withContainer: Boolean, errors: ErrorInfo*): RenderArgs = {
    RenderArgs(
      mevent = MEventTmp(
        etype   = etype,
        ownerId = args.request.producerId,
        id      = Some(replyTo)
      ),
      advExtTgs = Seq(args.target.target),
      errors    = errors,
      withContainer = withContainer
    )
  }

  /** Перезаписать содержимое блока цели на странице. */
  def renderEventReplace(rargs: RenderArgs): Unit = {
    val html = rargs.mevent.etype.render(rargs)
    val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = InnerHtmlById(replyTo, htmlStr)
    val cmd = JsCmd( jsa.renderToString() )
    sendCommand(cmd)
  }

  def renderInProcess(): Unit = {
    val etype = EventTypes.AdvExtTgInProcess
    val rargs = evtRenderArgs(etype, withContainer = true)
    val html = etype.render(rargs)
    val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = JsAppendById(RUNNER_EVENTS_DIV_ID, htmlStr)
    val cmd = JsCmd( jsa.renderToString() )
    sendCommand(cmd)
  }


  /** Рендер на экран уведомления об успехе, стерев предыдущую инфу по target'у. */
  def renderSuccess(): Unit = {
    val rargs = evtRenderArgs( EventTypes.AdvExtTgSuccess )
    renderEventReplace(rargs)
  }

  /** Сообщить юзеру, что на стороне js зафиксирована ошибка. */
  def renderError(msg: String, errOpt: Option[JsErrorInfo]): Unit = {
    val err = errOpt match {
      case Some(jserr) =>
        ErrorInfo(
          msg = jserr.msg,
          args = jserr.args,
          info = jserr.other.map(_.toString())
        )
      case None =>
        ErrorInfo(msg = msg)
    }
    val rargs = evtRenderArgs(EventTypes.AdvExtTgError, err)
    renderEventReplace(rargs)
  }

}
