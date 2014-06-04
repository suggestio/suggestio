package controllers

import util.PlayMacroLogsImpl
import util.acl.MaybeAuth
import util.qsb.SMJoinAnswers
import models._
import views.html.market.join._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.14 18:29
 * Description: Контроллер раздела сайта со страницами и формами присоединения к sio-market.
 */
object MarketJoin extends SioController with PlayMacroLogsImpl {
  import LOGGER._

  val joinQuestionsFormM = {
    import play.api.data._, Forms._
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
  def joinQuestionsForm = MaybeAuth { implicit request =>
    Ok(wifiJoinQuestionsFormTpl(joinQuestionsFormM))
  }

  /** Сабмит формы галочек для перехода на второй шаг. */
  def joinQuestionsFormSubmit = MaybeAuth { implicit request =>
    joinQuestionsFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("joinQuestionsFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(wifiJoinQuestionsFormTpl(formWithErrors))
      },
      {smja =>
        Redirect(???)
      }
    )
  }

  def joinForm(smja: SMJoinAnswers) = MaybeAuth { implicit request =>
    ???
  }

  def joinFormSubmit(smja: SMJoinAnswers) = MaybeAuth { implicit request =>
    ???
  }

}
