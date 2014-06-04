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

  private val joinQuestionsFormM: Form[SMJoinAnswers] = {
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
  def joinQuestionsForm(smjaOpt: Option[SMJoinAnswers]) = MaybeAuth { implicit request =>
    val form = smjaOpt.fold(joinQuestionsFormM) { joinQuestionsFormM fill }
    Ok(wifiJoinQuestionsFormTpl(form))
  }

  /** Сабмит формы галочек для перехода на второй шаг. */
  def joinQuestionsFormSubmit = MaybeAuth { implicit request =>
    joinQuestionsFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("joinQuestionsFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(wifiJoinQuestionsFormTpl(formWithErrors))
      },
      {smja =>
        Redirect(routes.MarketJoin.joinForm(smja))
      }
    )
  }


  private val joinFormM: Form[MInviteRequest] = {
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
  def joinForm(smja: SMJoinAnswers) = MaybeAuth { implicit request =>
    Ok(wifiJoinFormTpl(smja, joinFormM))
  }

  /** Сабмит запроса инвайта, в котором много полей. */
  def joinFormSubmit(smja: SMJoinAnswers) = MaybeAuth.async { implicit request =>
    joinFormM.bindFromRequest().fold(
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
          Redirect(routes.MarketJoin.joinRequestSuccess)
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
