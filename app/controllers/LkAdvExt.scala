package controllers

import java.net.URL

import models.adv._
import util.PlayMacroLogsImpl
import util.acl.{IsAdnNodeAdmin, CanAdvertiseAd}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import play.api.data._, Forms._
import util.FormUtil._
import views.html.lk.adv.ext._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 14:45
 * Description: Этот контроллер руководит взаимодейтсвием пользователя с системой размещения карточек в соц.сетях и
 * иных сервисах, занимающихся PR-деятельстью.
 * Логический родственник [[MarketAdv]], который занимается размещениями карточек на узлах.
 */
object LkAdvExt extends SioControllerImpl with PlayMacroLogsImpl {

  import LOGGER._


  /** Маппинг одной выбранной цели. */
  private def advM: Mapping[Option[String]] = {
    val t = tuple(
      "enabled"  -> optional(boolean),
      "targetId" -> esIdM
    )
    .transform[Option[String]] (
      { case (Some(true), targetId) => Some(targetId)
        case _ => None },
      { case Some(targetId) => (Some(true), targetId)
        case _ => (None, "") }
    )
    optional(t).transform[Option[String]] (
      _.flatten,
      {v => if (v.isDefined) Some(v) else None }
    )
  }

  /** Маппинг формы со списком целей. */
  private def advsFormM: Form[List[String]] = {
    Form(
      "adv" -> list(advM)
        .transform[List[String]] (_.flatten, _.map(Some.apply))
    )
  }


  /**
   * Запрос страницы с инфой по размещению указанной карточки в соц.сетях.
   * @param adId id рекламной карточки.
   * @return 200 Ок + страница с данными по размещениям на внешних сервисах.
   */
  def forAd(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    val targetsFut = MExtTarget.findByAdnId(request.producerId)
    val currentAdvsFut = MExtAdv.findForAd(adId)
      .map { advs => advs.iterator.map { a => a.extTargetId -> a }.toMap }
    val form = advsFormM
    for {
      targets       <- targetsFut
      currentAdvs   <- currentAdvsFut
    } yield {
      Ok(forAdTpl(request.mad, request.producer, targets, currentAdvs, form))
    }
  }

  /**
   * Сабмит формы размещения рекламных карточек на внешних сервисах.
   * Нужно запустить рендер карточки в картинку и совершить всякие прочие приготовления.
   * @param adId id размещаемой рекламной карточки.
   * @return 200 Ok со страницей деятельности по размещению.
   */
  def advFormSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    ???
  }


  /** Маппинг формы для ввода ссылки на цель. */
  def targetFormM: Form[MExtTarget] = {
    Form(
      // Проверять по доступным сервисам, подходит ли эта ссылка для хотя бы одного из них. И возвращать этот сервис.
      "url" -> urlM
        .transform[(URL, Option[MExtService])] (
          {url =>
            //val url1 = srv.normalizeTargetUrl(url)
            url -> MExtServices.findForHost(url.getHost)
          },
          { _._1 }
        )
        .verifying("error.service.unknown", _._2.isDefined)
        .transform [MExtTarget] (
          {case (url, srvOpt) =>
            val srv = srvOpt.get
            val url1 = srv.normalizeTargetUrl(url)
            MExtTarget(url = url1, service = srv, adnId = null)
          },
          { metgt => (new URL(metgt.url), Some(metgt.service)) }
        )
    )
  }

  /**
   * Запрос формы создания новой цели для размещения рекламы.
   * @param adnId id узла.
   * @return 200 Ok с отрендеренной формой.
   */
  def createTarget(adnId: String) = IsAdnNodeAdmin(adnId) { implicit request =>
    val form = targetFormM
    Ok(_createTargetTpl(request.adnNode, form))
  }

  def createTargetSubmit(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    targetFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"createTargetSubmit($adnId): Unable to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(_createTargetTpl(request.adnNode, formWithErrors))
      },
      {tgt0 =>
        // TODO Обращаться по ссылке, получать title страницы, вставлять в поле name
        val tgt1 = tgt0.copy(
          adnId = adnId
        )
        tgt1.save.map { tgtId =>
          Ok("TODO")
        }
      }
    )
  }

}
