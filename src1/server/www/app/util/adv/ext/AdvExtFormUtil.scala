package util.adv.ext

import java.net.URL

import javax.inject.Inject
import io.suggest.adv.ext.view.RunnerPage
import models.adv._
import models.mext.MExtService
import play.api.data.Forms._
import play.api.data._
import util.FormUtil.{esIdM, nameM, toStrOptM, urlM}
import util.ext.{ExtServicesUtil, IExtServiceHelper}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.15 18:58
  * Description: Adv Ext Form Util
  * Утиль для поддержки форм внешнего размещения.
  */
class AdvExtFormUtil @Inject()(
  extServicesUtil: ExtServicesUtil
) {

  /** id div'а в который надо рендерить события размещения. */
  // Сделать его deprecated?
  def RUNNER_EVENTS_DIV_ID = RunnerPage.ID_EVTS_CONTAINER

  /* С вложенными формами ext.adv есть проблемы: они вложенные. */

  /** Маппинг для ссылки на цель. */
  def tgFullUrlM = {
    urlM
      .transform[(URL, Option[IExtServiceHelper])] (
        {url =>
          url -> extServicesUtil.findForHost(url.getHost)
        },
        { _._1 }
      )
      .verifying("error.service.unknown", _._2.isDefined)
      .transform[(String, MExtService)] (
        {case (url, srvOpt) =>
          val srv = srvOpt.get
          val url1 = srv.normalizeTargetUrl(url)
          (url1, srv.mExtService)
        },
        {case (url, srv) =>
          (new URL(url), extServicesUtil.helperFor(srv))
        }
      )
  }

  def URL_FN = "url"
  def urlKM  = URL_FN -> tgFullUrlM
  def nameKM = "name" -> toStrOptM(nameM)
  def idKM   = "id"   -> optional(esIdM)

  /** Маппинг для данных одного target'а. */
  def targetM(adnId: String): Mapping[MExtTarget] = {
    mapping(urlKM, nameKM, idKM)
    {case ((url, srv), nameOpt, idOpt) =>
      MExtTarget(
        url     = url,
        service = srv,
        adnId   = adnId,
        id      = idOpt,
        name    = nameOpt
      )
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
  def oneTargetFullFormM(adnId: String): OneExtTgForm = {
    Form(oneTargetFullM(adnId))
  }

  def oneRawTargetFullFormM(adnId: String) = Form(
    TG_FN -> rawTargetM(adnId)
  )

  /**
   * Шаблоны для сборки дефолтовых форм на лету используют этот метод.
   * @param tg Экземпляр таргета.
   * @return Экземпляр Form'ы для работы с одной целью.
   */
  def formForTarget(tg: MExtTarget): OneExtTgForm = {
    oneTargetFullFormM(tg.adnId)
      .fill((tg, None))
  }

}


/** Интерфейс для DI-поля, содержащего инстанс [[AdvExtFormUtil]]. */
trait IAdvExtFormUtilDi {
  def advExtFormUtil    : AdvExtFormUtil
}
