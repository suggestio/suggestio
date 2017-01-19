package util.adv.ext.ut

import models.adv.{IExtAdvServiceActorArgs, JsExtTarget, MExtTargetInfoFull}
import models.adv.js.JsCmd
import models.event.{IErrorInfo, MEventTmp, MEventTypes, RenderArgs}
import models.mctx.IContextUtilDi
import play.api.libs.json.JsString
import util.adv.ext.IAdvExtFormUtilDi
import util.jsa.JsAppendById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 13:22
 * Description: поддержка рендера на экран юзеру разных сообщений от service-актора.
 */
trait SvcActorJsRenderUtil
  extends ISendCommand
  with IAdvExtFormUtilDi
  with IContextUtilDi
{

  /** Аргументы service-актора, переданные ему при запуске. */
  def args: IExtAdvServiceActorArgs

  /** Вызов рендера в браузер сообщения о проблеме инициализации текущего сервиса. */
  def serviceInitFailedRender(errors: Seq[IErrorInfo]): Unit = {
    val mevent = MEventTmp(
      etype       = MEventTypes.AdvServiceError,
      ownerId     = args.request.producer.id.get,
      isCloseable = false,
      isUnseen    = true
    )
    val rargs = RenderArgs(
      mevent        = mevent,
      withContainer = false,
      adnNodeOpt    = Some(args.request.producer),
      advExtTgs     = args.targets.map(_.target),
      madOpt        = Some(args.request.mad),
      extServiceOpt = Some(args.service),
      errors        = errors
    )
    val html = rargs.mevent.etype.render(rargs)(args.ctx)
    val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = JsAppendById(advExtFormUtil.RUNNER_EVENTS_DIV_ID, htmlStr)
    val jsCmd = JsCmd(
      jsCode = jsa.renderToString()
    )
    sendCommand(jsCmd)
  }


  /** Конвертация MExtTarget и данных returnTo в js-модель. */
  def tg2jsTg(tg: MExtTargetInfoFull): JsExtTarget = {
    JsExtTarget(
      id   = tg.target.id.get,
      url  = tg.target.url,
      name = tg.target.name,
      onClickUrl = {
        val urlCall = tg.returnTo.builder()
          .setAdnId( tg.target.adnId )
          .setFocusedAdId( args.request.mad.id.get )
          .setFocusedProducerId( args.request.producer.id.get )
          .toCall
        ctxUtil.toScAbsUrl( urlCall )
      }
    )
  }

}

