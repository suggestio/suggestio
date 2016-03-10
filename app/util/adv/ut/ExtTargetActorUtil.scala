package util.adv.ut

import models.adv.IExtAdvTargetActorArgs
import models.adv.ext.act.ExtTargetActorEnv
import models.adv.js.JsCmd
import models.adv.js.ctx.JsErrorInfo
import models.event._
import play.api.libs.json.JsString
import util.adv.ext.IAeFormUtilDi
import util.jsa.{InnerHtmlById, JsAppendById}

/** Утиль для сборки target-акторов. */
trait ExtTargetActorUtil
  extends ISendCommand
  with ReplyTo
  with ExtTargetActorEnv
  with IAeFormUtilDi
{

  override val args: IExtAdvTargetActorArgs

  import args.ctx

  def evtRenderArgs(etype: MEventType, errors: ErrorInfo*): RenderArgs = {
    evtRenderArgs(etype, withContainer = false, errors : _*)
  }
  def evtRenderArgs(etype: MEventType, withContainer: Boolean, errors: ErrorInfo*): RenderArgs = {
    RenderArgs(
      mevent = MEventTmp(
        etype   = etype,
        ownerId = args.request.producer.id.get,
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
    val etype = MEventTypes.AdvExtTgInProcess
    val rargs = evtRenderArgs(etype, withContainer = true)
    val html = etype.render(rargs)
    val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = JsAppendById( aeFormUtil.RUNNER_EVENTS_DIV_ID, htmlStr)
    val cmd = JsCmd( jsa.renderToString() )
    sendCommand(cmd)
  }


  /** Рендер на экран уведомления об успехе, стерев предыдущую инфу по target'у. */
  def renderSuccess(): Unit = {
    val rargs = evtRenderArgs( MEventTypes.AdvExtTgSuccess )
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
    val rargs = evtRenderArgs(MEventTypes.AdvExtTgError, err)
    renderEventReplace(rargs)
  }

}
