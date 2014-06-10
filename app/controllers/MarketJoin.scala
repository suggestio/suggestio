package controllers

import util.PlayMacroLogsImpl
import util.acl.MaybeAuth
import util.SiowebEsUtil.client
import models._
import views.html.market.join._
import util.FormUtil._
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.14 18:29
 * Description: Контроллер раздела сайта со страницами и формами присоединения к sio-market.
 */
object MarketJoin extends SioController with PlayMacroLogsImpl {
  import LOGGER._

  /** Маппинг формы анкеты с галочками про wi-fi. */
  private val wifiJoinQuestionsFormM: Form[SMJoinAnswers] = {
    val boolOpt = optional(boolean)
    Form(mapping(
      "haveWifi"        -> boolOpt,
      "fullCoverage"    -> boolOpt,
      "knownEquipment"  -> boolOpt,
      "altFw"           -> boolOpt,
      "isWrtFw"         -> boolOpt,
      "landlineInet"    -> boolOpt,
      "smallRoom"       -> boolOpt,
      "audienceSz"      ->
        optional(text(maxLength = 5))
        .transform[Option[AudienceSize]](
          { _.filter(!_.isEmpty).flatMap(AudienceSizes.maybeWithName) },
          { _.map(_.toString) }
        )
    )
    { SMJoinAnswers.apply }
    { SMJoinAnswers.unapply }
    )
  }

  /** Рендер страницы с формой, где можно расставить галочки, ответив на вопросы. */
  def wifiJoinQuestionsForm(smjaOpt: Option[SMJoinAnswers]) = MaybeAuth { implicit request =>
    val form = smjaOpt.fold(wifiJoinQuestionsFormM) { wifiJoinQuestionsFormM fill }
    Ok(wifiJoinQuestionsFormTpl(form))
  }

  /** Сабмит формы галочек для перехода на второй шаг. */
  def wifiJoinQuestionsFormSubmit = MaybeAuth { implicit request =>
    wifiJoinQuestionsFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("joinQuestionsFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(wifiJoinQuestionsFormTpl(formWithErrors))
      },
      {smja =>
        Redirect(routes.MarketJoin.wifiJoinForm(smja))
      }
    )
  }

  private val text2048M = text(maxLength = 2048).transform(strTrimSanitizeF, strIdentityF)

  /** Маппинг для формы забивания текстовых полей запроса инвайта на wi-fi узел. */
  private val wifiJoinFormM: Form[MirMeta] = {
    Form(
      mapping(
        "company"       -> companyNameM,
        "audienceDescr" -> toStrOptM(text2048M),
        "humanTraffic"  -> toStrOptM(text(maxLength = 1024).transform(strTrimSanitizeF, strIdentityF)),
        "address"       -> addressM,
        "siteUrl"       -> urlStrOptM,
        "phone"         -> phoneM,
        "payReqs"       -> optional(text(maxLength = 2048))
      )
      {(company, audienceDescr, humanTraffic, address, siteUrl, phone, payReqs) =>
        MirMeta(
          company = company, audienceDescr = audienceDescr, humanTraffic = humanTraffic,
          address = address, siteUrl = siteUrl, contactPhone = phone
        )
      }
      {mirMeta =>
        import mirMeta._
        Some((company, audienceDescr, humanTraffic, address, siteUrl, contactPhone, payReqs))
      }
    )
  }


  /** Рендер формы запроса подключения, которая содержит разные поля для ввода текстовой информации. */
  def wifiJoinForm(smja: SMJoinAnswers) = MaybeAuth { implicit request =>
    Ok(wifiJoinFormTpl(smja, wifiJoinFormM))
  }

  /** Сабмит запроса инвайта, в котором много полей. */
  def wifiJoinFormSubmit(smja: SMJoinAnswers) = MaybeAuth.async { implicit request =>
    wifiJoinFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("joinFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(wifiJoinFormTpl(smja, formWithErrors))
      },
      {mirMeta =>
        val mir = MInviteRequest(reqType = InviteReqTypes.Wifi, meta = mirMeta, joinAnswers = Some(smja))
        mir.save.map { mirSavedRdr }
      }
    )
  }

  /** Куда отправлять юзера, когда его запрос сохранён? */
  private def mirSavedRdr(mirId: String) = {
    Redirect(routes.MarketJoin.joinRequestSuccess())
      .flashing("success" -> "Ваш запрос на подключение к системе принят.")
  }

  /** Отобразить страничку с писаниной о том, что всё ок. */
  def joinRequestSuccess = MaybeAuth { implicit request =>
    Ok(joinSuccessTpl())
  }


  private val advJoinFormM: Form[MirMeta] = {
    Form(
      mapping(
        "company"   -> companyNameM,
        "info"      -> text2048M.transform[Option[String]](Option(_).filter(!_.isEmpty), _ getOrElse ""),
        "address"   -> addressM,
        "floor"     -> floorOptM,
        "section"   -> sectionOptM,
        "siteUrl"   -> urlStrOptM,
        "phone"     -> phoneM
      )
      {(company, info, address, floor, section, siteUrl, phone) =>
        MirMeta(
          company = company, info = info, address = address, floor = floor, section = section,
          siteUrl = siteUrl, contactPhone = phone
        )
      }
      {mirMeta =>
        import mirMeta._
        Some((company, info, address, floor, section, siteUrl, contactPhone))
      }
    )
  }


  /** Юзер хочется зарегаться как рекламное агентство. Отрендерить страницу с формой, похожей на форму
    * заполнения сведений по wi-fi сети. */
  def joinAdvRequest = MaybeAuth { implicit request =>
    Ok(joinAdvTpl(advJoinFormM))
  }

  /**
   * Сабмит формы запроса присоединения к системе в качестве рекламодателя.
   * @return Редирект или 406 NotAcceptable.
   */
  def joinAdvRequestSubmit = MaybeAuth.async { implicit request =>
    advJoinFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("joinAdvRequestSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(joinAdvTpl(formWithErrors))
      },
      {mirMeta =>
        val mir = MInviteRequest(reqType = InviteReqTypes.Adv, meta = mirMeta)
        mir.save.map { mirSavedRdr }
      }
    )
  }

}
