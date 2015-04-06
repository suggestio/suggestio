package util.adv

import java.net.URL
import io.suggest.adv.ext.view.RunnerPage
import models.adv._
import models.adv.js.{JsCmd, IWsCmd}
import models.event.{RenderArgs, MEventTmp, IErrorInfo}
import play.api.data._, Forms._
import play.api.libs.json.JsString
import util.FormUtil
import util.FormUtil.{urlM, esIdM}
import util.event.EventTypes
import util.jsa.JsAppendById

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 18:58
 * Description: Утиль для поддержки внешнего размещения.
 */
object ExtUtil {

  /** id div'а в который надо рендерить события размещения. */
  // Сделать его deprecated?
  def RUNNER_EVENTS_DIV_ID = RunnerPage.ID_EVTS_CONTAINER

  /* С вложенными формами ext.adv есть проблемы: они вложенные. */

  /** Маппинг для ссылки на цель. */
  def tgFullUrlM = {
    urlM
      .transform[(URL, Option[MExtService])] (
        {url =>
          url -> MExtServices.findForHost(url.getHost)
        },
        { _._1 }
      )
      .verifying("error.service.unknown", _._2.isDefined)
      .transform[(String, MExtService)] (
        {case (url, srvOpt) =>
          val srv = srvOpt.get
          val url1 = srv.normalizeTargetUrl(url)
          (url1, srv) },
        {case (url, srv) =>
          (new URL(url), Some(srv)) }
      )
  }

  def URL_FN = "url"
  def urlKM  = URL_FN -> tgFullUrlM
  def nameKM = "name" -> FormUtil.toStrOptM(FormUtil.nameM)
  def idKM   = "id"   -> optional(esIdM)

  /** Маппинг для данных одного target'а. */
  def targetM(adnId: String): Mapping[MExtTarget] = {
    mapping(urlKM, nameKM, idKM)
    {case ((url, srv), nameOpt, idOpt) =>
      MExtTarget(url = url, service = srv, adnId = adnId, id = idOpt, name = nameOpt)
    }
    {tg =>
      val res = ((tg.url, tg.service), tg.name, tg.id)
      Some(res)
    }
  }

  /** Когда target не нужен, а нужен сырой доступ к данным маппинга, можно задействовать это. */
  def rawTargetM(adnId: String) = tuple(URL_FN -> text, nameKM, idKM)

  def TG_FN    = "tg"
  def returnKM = "return" -> optional(MExtReturns.mapping)

  /** Полный маппинг для одной цели вместе с настройками return'а. */
  def oneTargetFullM(adnId: String) = {
    tuple(
      TG_FN     -> targetM(adnId),
      returnKM
    )
  }

  /**
   * Маппинг формы для ввода ссылки на цель.
   * @param adnId id узла, в рамках которого происходит действо.
   * @return Экземпляр формы.
   */
  def oneTargetFullFormM(adnId: String) = Form(oneTargetFullM(adnId))

  def oneRawTargetFullFormM(adnId: String) = Form(
    TG_FN -> rawTargetM(adnId)
  )

  /**
   * Шаблоны для сборки дефолтовых форм на лету используют этот метод.
   * @param tg Экземпляр таргета.
   * @return Экземпляр Form'ы для работы с одной целью.
   */
  def formForTarget(tg: MExtTarget) = oneTargetFullFormM(tg.adnId) fill (tg, None)

}


/** Интерфейс для отправки одной команды по ws. */
trait ISendCommand {
  /** Отправка указанной js-команды по ws в браузер клиенту. */
  def sendCommand(cmd: IWsCmd): Unit
}

/** Реализация [[ISendCommand]] через промежуточный актор (актор-медиатор). */
trait MediatorSendCommand extends ISendCommand {

  def args: WsMediatorRef

  def sendCommand(cmd: IWsCmd): Unit = {
    args.wsMediatorRef ! cmd
  }

}


/** Утиль для построения */
trait ExtServiceActorUtil extends ISendCommand {

  /** Аргументы service-актора, переданные ему при запуске. */
  def args: IExtAdvServiceActorArgs

  /** Вызов рендера в браузер сообщения о проблеме инициализации текущего сервиса. */
  def serviceInitFailedRender(errors: Seq[IErrorInfo]): Unit = {
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
      extServiceOpt = Some(args.service),
      errors        = errors
    )
    val html = rargs.mevent.etype.render(rargs)(args.ctx)
    val htmlStr = JsString(html.body) // TODO Вызывать для рендера туже бадягу, что и контроллер вызывает.
    val jsa = JsAppendById(ExtUtil.RUNNER_EVENTS_DIV_ID, htmlStr)
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
      onClickUrl = tg.returnTo.builder()
        .setAdnId( tg.target.adnId )
        .setFocusedAdId( args.request.mad.id.get )
        .setFocusedProducerId( args.request.producerId )
        .toAbsUrl
    )
  }

}
