package controllers

import util.PlayMacroLogsImpl
import util.acl.MaybeAuth
import util.qsb.SMJoinAnswers
import util.SiowebEsUtil.client
import models._
import views.html.market.join._
import play.api.data.Form
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


  /** Маппинг для формы забивания текстовых полей запроса инвайта на wi-fi узел. */
  private val wifiJoinFormM: Form[MInviteRequest] = {
    val text2048 = text(maxLength = 2048).transform(strTrimSanitizeF, strIdentityF)
    Form(
      mapping(
        "company"       -> companyNameM,
        "audienceDescr" -> text2048,
        "humanTraffic"  -> text(maxLength = 1024).transform(strTrimSanitizeF, strIdentityF),
        "address"       -> addressM,
        "siteUrl"       -> urlStrOptM,
        "phone"         -> phoneM,
        "info"          -> text2048.transform[Option[String]](Option(_).filter(!_.isEmpty), _ getOrElse "")
      )
      {(company, audienceDescr, humanTraffic, address, siteUrl, phone, info) =>
        MInviteRequest(
          company = company, audienceDescr = audienceDescr, humanTraffic = humanTraffic,
          address = address, siteUrl = siteUrl, contactPhone = phone, info = info
        )
      }
      {mir =>
        import mir._
        Some((company, audienceDescr, humanTraffic, address, siteUrl, contactPhone, info))
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
      {mir =>
        val mir1 = mir.copy(
          haveWifi      = smja.haveWifi,
          fullCoverage  = smja.fullCoverage,
          knownEquip    = smja.knownEquipment,
          altFw         = smja.altFw,
          isWrtFw       = smja.isWrtFw,
          landlineInet  = smja.landlineInet,
          smallRoom     = smja.smallRoom,
          audienceSz    = smja.audienceSz
        )
        mir1.save.map { mirId =>
          Redirect(routes.MarketJoin.joinRequestSuccess())
            .flashing("success" -> "Ваш запрос на подключение к системе принят.")
        }
      }
    )
  }

  /** Отобразить страничку с писаниной о том, что всё ок. */
  def joinRequestSuccess = MaybeAuth { implicit request =>
    Ok(joinSuccessTpl())
  }

}
