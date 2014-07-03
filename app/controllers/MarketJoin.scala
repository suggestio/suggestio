package controllers

import org.joda.time.DateTime
import play.api.i18n.Messages
import util.billing.MmpDailyBilling
import util.img.{ImgFormUtil, OrigImgIdKey}
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
import util.img.GalleryUtil._

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


  /** Маппинг для формы забивания текстовых полей запроса инвайта на wi-fi узел. */
  private val wifiJoinFormM = {
    Form(
      mapping(
        "company"         -> companyNameM,
        "audienceDescr"   -> audienceDescrM,
        "humanTrafficAvg" -> humanTrafficAvgM,
        "address"         -> addressM,
        "siteUrl"         -> urlStrOptM,
        "phone"           -> phoneM,
        "payReqs"         -> optional(text(maxLength = 2048)),  // TODO Парсить и проверять
        "email"           -> email,
        galleryKM,
        CAPTCHA_ID_FN     -> Captcha.captchaIdM,
        CAPTCHA_TYPED_FN  -> Captcha.captchaTypedM
      )
      {(companyName, audienceDescr, humanTrafficAvg, address, siteUrl, phone, payReqs, email1, galleryIks, _, _) =>
        val mir = applyForm(companyName = companyName, audienceDescr = Some(audienceDescr),
          humanTrafficAvg = Some(humanTrafficAvg), address = address, siteUrl = siteUrl, phone = phone,
          payReqs = payReqs, email1 = email1, anmt = AdNetMemberTypes.MART, withMmp = true,
          reqType = InviteReqTypes.Wifi)
        (mir, galleryIks)
      }
      {case (mir, galleryIks) =>
        // unapply() вызывается только когда всё в Left, т.е. при ошибка заполнения форм.
        val companyName = unapplyCompanyName(mir)
        val audienceDescr = unapplyAudDescr(mir)
        val humanTraffic = unapplyHumanTraffic(mir)
        val address = unapplyAddress(mir)
        val siteUrl = unapplySiteUrl(mir)
        val officePhone = unapplyOfficePhone(mir)
        val payReqs = unapplyPayReqs(mir)
        val email1 = unapplyEmail(mir)
        Some((companyName, audienceDescr, humanTraffic, address, siteUrl, officePhone, payReqs, email1, galleryIks, "", ""))
      }
    )
  }

  private def unapplyCompanyName(mir: MInviteRequest): String = {
    mir.company
      .left.map(_.meta.name)
      .left.getOrElse { mir.adnNode.left.map(_.meta.name).left getOrElse "" }
  }
  private def unapplyAudDescr(mir: MInviteRequest): String = {
    mir.adnNode
      .left.map(_.meta.audienceDescr)
      .left.getOrElse(None)
      .getOrElse("")
  }
  private def unapplyHumanTraffic(mir: MInviteRequest): Int = {
    mir.adnNode
      .left.map(_.meta.humanTrafficAvg)
      .left.getOrElse(None)
      .getOrElse(0)
  }
  private def unapplyAddress(mir: MInviteRequest): String = {
    mir.adnNode
      .left.map(_.meta.address)
      .left.getOrElse(None)
      .getOrElse("")
  }
  private def unapplySiteUrl(mir: MInviteRequest): Option[String] = {
    mir.adnNode
      .left.map(_.meta.siteUrl)
      .left.getOrElse(None)
  }
  private def unapplyOfficePhone(mir: MInviteRequest): String = {
    mir.company
      .left.map(_.meta.officePhones.headOption)
      .left.getOrElse(None)
      .getOrElse("")
  }
  private def unapplyPayReqs(mir: MInviteRequest): Option[String] = {
    mir.payReqs
      .flatMap { _.left.map(_.toString).fold(Some.apply, { _ => None }) }
  }
  private def unapplyEmail(mir: MInviteRequest): String = {
    mir.emailAct
      .left.map(_.email)
      .left.getOrElse("")
  }
  private def unapplyInfo(mir: MInviteRequest): Option[String] = {
    mir.adnNode
      .left.map(_.meta.info)
      .left.getOrElse(None)
  }
  private def unapplyGallery(mir: MInviteRequest): List[OrigImgIdKey] = {
    mir.adnNode
      .left.map { adnNode => gallery2iiks(adnNode.gallery) }
      .left.getOrElse(Nil)
  }


  /** Накатывание результатов маппинга формы на новый экземпляр [[models.MInviteRequest]]. */
  private def applyForm(companyName: String, audienceDescr: Option[String] = None, humanTrafficAvg: Option[Int] = None,
    address: String, siteUrl: Option[String], phone: String, payReqs: Option[String] = None, email1: String,
    anmt: AdNetMemberType, withMmp: Boolean, reqType: InviteReqType, info: Option[String] = None): MInviteRequest = {
    val company = MCompany(
      meta = MCompanyMeta(name = companyName, officePhones = List(phone))
    )
    val node = MAdnNode(
      companyId = "",
      meta = AdnMMetadata(
        name            = companyName,
        address         = Option(address),
        siteUrl         = siteUrl,
        humanTrafficAvg = humanTrafficAvg,
        audienceDescr   = audienceDescr,
        info            = info
      ),
      personIds = Set.empty,
      adn = anmt.getAdnInfoDflt
    )
    val eact = EmailActivation(email1)
    val mbc = MBillContract(
      adnId = "",
      contractDate = DateTime.now,
      suffix = Option(MmpDailyBilling.CONTRACT_SUFFIX_DFLT),
      isActive = true
    )
    val mbb = MBillBalance(adnId = "", amount = 0F)
    val mmp: Option[Either[MBillMmpDaily, Int]] = if (withMmp) {
      // TODO Использовать формулу для рассчёта значений тарифов на основе человеч.трафика
      // TODO Использовать конфиг для хранения дефолтовых значений.
      val mmp = MBillMmpDaily(
        contractId = -1,
        mmpWeekday = 10F,
        mmpWeekend = 15F,
        mmpPrimetime = 20F,
        onRcvrCat = 2.0F,
        onStartPage = 4.0F,
        weekendCalId = "", // TODO
        primeCalId = "" // TODO
      )
      Some(Left(mmp))
    } else {
      None
    }
    MInviteRequest(
      name      = companyName + " от " + email1,
      reqType   = InviteReqTypes.Wifi,
      company   = Left(company),
      adnNode   = Left(node),
      contract  = Left(mbc),
      mmp       = mmp,
      balance   = Left(mbb),
      emailAct  = Left(eact),
      payReqs   = None // TODO Нужно сохранить сюда платёжные атрибуты
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
      {case (mir, galleryIiks) =>
        assert(mir.adnNode.isLeft, "error.mir.adnNode.not.isLeft")
        // Схоронить картинки.
        ImgFormUtil.updateOrigImgFull(
          needImgs = gallery4s(galleryIiks),
          oldImgs = Nil
        ) flatMap { savedImgs =>
          // Картинки сохранены. Обновить рекламный узел.
          val mir2 = mir.copy(
            joinAnswers = Some(smja),
            adnNode = mir.adnNode.left.map { adnNode0 =>
              adnNode0.copy(
                gallery = gallery2filenames(savedImgs)
              )
            }
          )
          mir2.save.map { irId =>
            sendEmailNewIR(irId, mir)
            rmCaptcha(formBinded, mirSavedRdr(irId))
          }
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


  /** Подключение в качестве рекламного агента, источника рекламы. */
  private val advJoinFormM: Form[MInviteRequest] = {
    Form(
      mapping(
        "company"   -> companyNameM,
        "info"      -> text2048M
          .transform[Option[String]]({str => emptyStrOptToNone(Option(str))}, strOptGetOrElseEmpty),
        "address"   -> addressM,
        "siteUrl"   -> urlStrOptM,
        "phone"     -> phoneM,
        "email"     -> email,
        CAPTCHA_ID_FN    -> Captcha.captchaIdM,
        CAPTCHA_TYPED_FN -> Captcha.captchaTypedM
      )
      {(companyName, info, address, siteUrl, phone, email1, _, _) =>
        applyForm(companyName = companyName, address = address, siteUrl = siteUrl,
          phone = phone, email1 = email1, anmt = AdNetMemberTypes.SHOP, withMmp = false,
          reqType = InviteReqTypes.Adv, info = info)
      }
      {mir =>
        val companyName = unapplyCompanyName(mir)
        val info = unapplyInfo(mir)
        val address = unapplyAddress(mir)
        val siteUrl = unapplySiteUrl(mir)
        val officePhone = unapplyOfficePhone(mir)
        val email1 = unapplyEmail(mir)
        Some((companyName, info, address, siteUrl, officePhone, email1, "", ""))
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
      {mir =>
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
