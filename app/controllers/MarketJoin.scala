package controllers

import org.joda.time.DateTime
import play.api.i18n.Messages
import util.billing.MmpDailyBilling
import util.{ContextImpl, PlayMacroLogsImpl}
import util.acl.{AbstractRequestWithPwOpt, MaybeAuth}
import util.SiowebEsUtil.client
import models._
import views.html.market.join._
import util.FormUtil._
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.{current, configuration}
import play.api.mvc.RequestHeader

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.14 18:29
 * Description: Контроллер раздела сайта со страницами и формами присоединения к sio-market.
 */
object MarketJoin extends SioController with PlayMacroLogsImpl with CaptchaValidator {
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
  private val wifiJoinFormM: Form[MInviteRequest] = {
    Form(
      mapping(
        "company"         -> companyNameM,
        "audienceDescr"   -> text2048M,
        "humanTrafficAvg" -> number(min = 10),
        "address"         -> addressM,
        "siteUrl"         -> urlStrOptM,
        "phone"           -> phoneM,
        "payReqs"         -> optional(text(maxLength = 2048)),
        "email"           -> email,
        CAPTCHA_ID_FN     -> Captcha.captchaIdM,
        CAPTCHA_TYPED_FN  -> Captcha.captchaTypedM
      )
      {(companyName, audienceDescr, humanTrafficAvg, address, siteUrl, phone, payReqs, email1, _, _) =>
        val company = MCompany(
          meta = MCompanyMeta(name = companyName, officePhones = List(phone))
        )
        val node = MAdnNode(
          companyId = "",
          meta = AdnMMetadata(
            name = companyName,
            address = Some(address),
            siteUrl = siteUrl,
            humanTrafficAvg = Some(humanTrafficAvg),
            audienceDescr = Option(audienceDescr)
          ),
          personIds = Set.empty,
          adn = AdNetMemberTypes.MART.getAdnInfoDflt
        )
        val eact = EmailActivation(email1)
        val mbc = MBillContract(adnId = "", contractDate = DateTime.now, suffix = Option(MmpDailyBilling.CONTRACT_SUFFIX_DFLT), isActive = true)
        val mbb = MBillBalance(adnId = "", amount = 0F)
        // TODO Использовать формулу для рассчёта значений тарифов на основе человеч.трафика
        // TODO Использовать конфиг для хранения дефолтовых значений.
        val mmp = MBillMmpDaily(
          contractId = -1,
          mmpWeekday = 10F,
          mmpWeekend = 15F,
          mmpPrimetime = 20F,
          onRcvrCat = 2.0F,
          onStartPage = 4.0F,
          weekendCalId = "",    // TODO
          primeCalId = ""       // TODO
        )
        MInviteRequest(
          reqType = InviteReqTypes.Wifi,
          company = Left(company),
          adnNode = Left(node),
          contract = Left(mbc),
          mmp = Some(Left(mmp)),
          balance = Left(mbb),
          emailAct = Left(eact),
          payReqs = None // TODO Нужно отмаппить платёжные атрибуты
        )
      }
      {mir =>
        // unapply() вызывается только когда всё в Left, т.е. при ошибка заполнения форм.
        val companyName: String = mir.company
          .left.map(_.meta.name)
          .left.getOrElse { mir.adnNode.left.map(_.meta.name).left getOrElse "" }
        val audienceDescr: String = mir.adnNode
          .left.map(_.meta.audienceDescr)
          .left.getOrElse(None)
          .getOrElse("")
        val humanTraffic: Int = mir.adnNode
          .left.map(_.meta.humanTrafficAvg)
          .left.getOrElse(None)
          .getOrElse(0)
        val address: String = mir.adnNode
          .left.map(_.meta.address)
          .left.getOrElse(None)
          .getOrElse("")
        val siteUrl: Option[String] = mir.adnNode
          .left.map(_.meta.siteUrl)
          .getOrElse(None)
        val officePhone: String = mir.company
          .left.map(_.meta.officePhones.headOption)
          .getOrElse(None).getOrElse("")
        val payReqs: Option[String] = mir.payReqs
          .flatMap { _.left.map(_.toString).fold(Some.apply, { _ => None }) }
        val email1: String = mir.emailAct
          .left.map(_.email)
          .left.getOrElse("")
        Some((companyName, audienceDescr, humanTraffic, address, siteUrl, officePhone, payReqs, email1, "", ""))
      }
    )
  }


  /** Рендер формы запроса подключения, которая содержит разные поля для ввода текстовой информации. */
  def wifiJoinForm(smja: SMJoinAnswers) = MaybeAuth { implicit request =>
    Ok(wifiJoinFormTpl(smja, wifiJoinFormM))
  }

  /** Сабмит запроса инвайта, в котором много полей. */
  def wifiJoinFormSubmit(smja: SMJoinAnswers) = MaybeAuth.async { implicit request =>
    val formBinded = checkCaptcha( wifiJoinFormM.bindFromRequest() )
    formBinded.fold(
      {formWithErrors =>
        debug("joinFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(wifiJoinFormTpl(smja, formWithErrors))
      },
      {mirMeta =>
        val mir = MInviteRequest(reqType = InviteReqTypes.Wifi, meta = mirMeta, joinAnswers = Some(smja))
        mir.save.map { irId =>
          sendEmailNewIR(irId, mir)
          rmCaptcha(formBinded, mirSavedRdr(irId))
        }
      }
    )
  }

  /** Куда отправлять юзера, когда его запрос сохранён? */
  private def mirSavedRdr(mirId: String)(implicit request: AbstractRequestWithPwOpt[_]) = {
    Redirect(routes.MarketJoin.joinRequestSuccess())
      .flashing("success" -> Messages("Your.IR.accepted"))
  }

  /** Отобразить страничку с писаниной о том, что всё ок. */
  def joinRequestSuccess = MaybeAuth { implicit request =>
    Ok(joinSuccessTpl())
  }


  private val advJoinFormM: Form[MirMeta] = {
    Form(
      mapping(
        "company"   -> companyNameM,
        //"info"      -> text2048M.transform[Option[String]](Option(_).filter(!_.isEmpty), _ getOrElse ""),
        "address"   -> addressM,
        "floor"     -> floorOptM,
        "section"   -> sectionOptM,
        "siteUrl"   -> urlStrOptM,
        "phone"     -> phoneM,
        "email"     -> email,
        CAPTCHA_ID_FN    -> Captcha.captchaIdM,
        CAPTCHA_TYPED_FN -> Captcha.captchaTypedM
      )
      {(company, address, floor, section, siteUrl, phone, email1, _, _) =>
        MirMeta(
          company = company, address = address, floor = floor, section = section,
          siteUrl = siteUrl, officePhone = phone, email = email1
        )
      }
      {mirMeta =>
        import mirMeta._
        Some((company, address, floor, section, siteUrl, officePhone, mirMeta.email, "", ""))
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
    val formBinded = checkCaptcha( advJoinFormM.bindFromRequest() )
    formBinded.fold(
      {formWithErrors =>
        debug("joinAdvRequestSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(joinAdvTpl(formWithErrors))
      },
      {mirMeta =>
        val mir = MInviteRequest(reqType = InviteReqTypes.Adv, meta = mirMeta)
        mir.save.map { irId =>
          sendEmailNewIR(irId, mir)
          rmCaptcha(formBinded, mirSavedRdr(irId))
        }
      }
    )
  }


  /** Отправить письмецо администрации s.io с ссылой на созданный запрос. */
  private def sendEmailNewIR(irId: String, mir0: MInviteRequest)(implicit request: RequestHeader) {
    val suEmailsConfKey = "market.join.request.notify.superusers.emails"
    val emails: Seq[String] = configuration.getStringSeq(suEmailsConfKey).getOrElse {
      // Нет ключа, уведомить разработчика, чтобы он настроил конфиг.
      error("""I don't know, whom to notify about new invite request. Add setting into your application.conf:\n  " + suEmailsConfKey + " = ["support@sugest.io"]""")
      Seq("support@suggest.io")
    }
    val mailMsg = use[MailerPlugin].email
    mailMsg.setRecipient(emails : _*)
    mailMsg.setFrom("no-reply@suggest.io")
    mailMsg.setSubject("Новый запрос на подключение | Suggest.io")
    val ctx = ContextImpl()
    val mir1 = mir0.copy(id = Some(irId))
    mailMsg.send(
      bodyText = views.txt.sys1.market.invreq.emailNewIRCreatedTpl(mir1)(ctx)
    )
  }

}
