package util.adv

import java.net.URL
import models.adv._
import models.adv.js.JsCommand
import play.api.data._, Forms._
import util.FormUtil
import util.FormUtil.{urlM, esIdUuidM}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 18:58
 * Description: Утиль для поддержки внешнего размещения.
 */
object ExtUtil {

  /** id div'а в который надо рендерить события размещения. */
  val RUNNER_EVENTS_DIV_ID = "adv-events"

  /* С вложенными формами ext.adv есть проблемы: они вложенные. */

  /** Маппинг для данных одного target'а. */
  def targetM(adnId: String): Mapping[MExtTarget] = {
    val tgUrlM = urlM
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
    mapping(
      "url"   -> tgUrlM,
      "name"  -> FormUtil.toStrOptM(FormUtil.nameM),
      "id"    -> optional(esIdUuidM)
    )
    {case ((url, srv), nameOpt, idOpt) =>
      MExtTarget(url = url, service = srv, adnId = adnId, id = idOpt, name = nameOpt)
    }
    {tg =>
      val res = ((tg.url, tg.service), tg.name, tg.id)
      Some(res)
    }
  }

  /** Маппинг для одной цели вместе с настройками return'а. */
  def oneTargetFullM(adnId: String) = {
    tuple(
      "tg"      -> targetM(adnId),
      "return"  -> optional(MExtReturns.mapping)
    )
  }

  /**
   * Маппинг формы для ввода ссылки на цель.
   * @param adnId id узла, в рамках которого происходит действо.
   * @return Экземпляр формы.
   */
  def oneTargetFullFormM(adnId: String) = Form(oneTargetFullM(adnId))

  /**
   * Шаблоны для сборки дефолтовых форм на лету используют этот метод.
   * @param tg Экземпляр таргета.
   * @return Экземпляр Form'ы для работы с одной целью.
   */
  def formForTarget(tg: MExtTarget) = oneTargetFullFormM(tg.adnId) fill (tg, None)

}


/** Поддержка sendCommand() через промежуточный актор. */
trait MediatorSendCommand {

  def args: WsMediatorRef

  def sendCommand(jsc: JsCommand): Unit = {
    args.wsMediatorRef ! jsc
  }

}
