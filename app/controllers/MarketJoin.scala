package controllers

import models.CallBackReqCallTimes.CallBackReqCallTime
import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import org.joda.time.DateTime
import play.api.i18n.Messages
import play.api.libs.iteratee.Enumerator
import util.billing.MmpDailyBilling
import util.img._
import util.mail.MailerWrapper
import util.PlayMacroLogsImpl
import util.acl.{AbstractRequestWithPwOpt, MaybeAuth}
import util.SiowebEsUtil.client
import models._
import views.html.market.join._
import util.FormUtil._
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}
import play.api.mvc.RequestHeader
import MarketLkAdnEdit.logoKM
import SioControllerUtil.PROJECT_CODE_LAST_MODIFIED

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.14 18:29
 * Description: Контроллер раздела сайта со страницами и формами присоединения к sio-market.
 */
object MarketJoin extends SioController with PlayMacroLogsImpl with CaptchaValidator with SiteMapXmlCtl {

  import LOGGER._


  /** Маппинг формы запроса обратного звонка с капчей, именем, телефоном и временем прозвона. */
  private def callbackRequestFormM = {
    Form(
      mapping(
        "name"  -> nameM,
        "phone" -> phoneM,
        "callTime" -> CallBackReqCallTimes.mapping,
        CAPTCHA_ID_FN     -> Captcha.captchaIdM,
        CAPTCHA_TYPED_FN  -> Captcha.captchaTypedM
      )
      {(name, phone, callTime, _, _) =>
        val mcMeta = MCompanyMeta(
          name          = name,
          officePhones  = List(phone),
          callTimeStart = Some(callTime.timeStart),
          callTimeEnd   = Some(callTime.timeEnd)
        )
        val mc = MCompany(mcMeta)
        MInviteRequest(
          name = s"Запрос звонка от '$name' тел $phone",
          reqType = InviteReqTypes.Adv,
          company = Left(mc)
        )
      }
      {mir =>
        val name = unapplyCompanyName(mir)
        val phone = unapplyOfficePhone(mir)
        val callTime: CallBackReqCallTime = mir.company
          .left.map { mc =>
            mc.meta.callTimeStart.flatMap { callTimeStart =>
              mc.meta.callTimeEnd.flatMap { callTimeEnd =>
                CallBackReqCallTimes.forTimes(callTimeStart, callTimeEnd)
              }
            }
          }
          .left.getOrElse(None)
          .getOrElse(CallBackReqCallTimes.values.head)
        Some((name, phone, callTime, "", ""))
      }
    )
  }

  /** Рендер страницы с формой обратного звонка. */
  def callbackRequest = MaybeAuth { implicit request =>
    cacheControlShort {
      Ok(callbackRequestTpl(callbackRequestFormM))
    }
  }

  /**
   * Сабмит формы запроса обратного вызова.
   * @return
   */
  def callbackRequestSubmit = MaybeAuth.async { implicit request =>
    val formBinded = checkCaptcha( callbackRequestFormM.bindFromRequest() )
    formBinded.fold(
      {formWithErrors =>
        debug("callbackRequestSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable(callbackRequestTpl(formWithErrors))
      },
      {mir =>
        mir.save.map { irId =>
          sendEmailNewIR(irId, mir)
          rmCaptcha(formBinded) {
            Ok( callbackRequestAcceptedTpl(mir.company.left.get.meta) )
          }
        }
      }
    )
  }



  private def colorOptKM  = "color"   -> colorOptM
  private def companyKM   = "company" -> companyNameM
  private def townKM      = "town"    -> townSomeM

  private def unapplyCompanyName(mir: MInviteRequest): String = {
    mir.company
      .left.map(_.meta.name)
      .left.getOrElse {
        mir.adnNode
          .fold("") { _.left.map(_.meta.name).left.getOrElse("") }
      }
  }
  private def unapplyTown(mir: MInviteRequest): Option[String] = {
    mir.adnNode.flatMap {
       _.left.map(_.meta.town)
        .left.getOrElse(None)
    }
  }

  private def unapplyAudDescr(mir: MInviteRequest): String = {
    mir.adnNode
      .flatMap {
        _.left.map(_.meta.audienceDescr)
         .left.getOrElse(None)
      }
      .getOrElse("")
  }
  private def unapplyHumanTraffic(mir: MInviteRequest): Int = {
    mir.adnNode
      .flatMap {
         _.left.map(_.meta.humanTrafficAvg)
          .left.getOrElse(None)
      }
      .getOrElse(0)
  }
  private def unapplyAddress(mir: MInviteRequest): String = {
    mir.adnNode
      .flatMap {
        _.left.map(_.meta.address)
         .left.getOrElse(None)
      }
      .getOrElse("")
  }
  private def unapplySiteUrl(mir: MInviteRequest): Option[String] = {
    mir.adnNode
      .flatMap {
        _.left.map(_.meta.siteUrl)
         .left.getOrElse(None)
      }
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
    mir.emailAct.fold("") {
       _.left.map(_.email)
        .left.getOrElse("")
    }
  }
  private def unapplyInfo(mir: MInviteRequest): Option[String] = {
    mir.adnNode.flatMap {
       _.left.map(_.meta.info)
        .left.getOrElse(None)
    }
  }
  private def unapplyColor(mir: MInviteRequest): Option[String] = {
    mir.adnNode.flatMap {
       _.left.map(_.meta.color)
        .left.getOrElse(None)
    }
  }


  /** Накатывание результатов маппинга формы на новый экземпляр MInviteRequest. */
  private def applyForm(companyName: String, audienceDescr: Option[String] = None, humanTrafficAvg: Option[Int] = None,
    address: String, siteUrl: Option[String], phone: String, payReqs: Option[String] = None, email1: String, town: Option[String],
    anmt: AdNetMemberType, withMmp: Boolean, reqType: InviteReqType, info: Option[String] = None, color: Option[String]): MInviteRequest = {
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
        info            = info,
        color           = color,
        town            = town
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
      val mmp = SysMarketBillingMmp.defaultMmpDaily
      Some(Left(mmp))
    } else {
      None
    }
    MInviteRequest(
      name      = companyName + " от " + email1,
      reqType   = reqType,
      company   = Left(company),
      adnNode   = Some(Left(node)),
      contract  = Some(Left(mbc)),
      mmp       = mmp,
      balance   = Some(Left(mbb)),
      emailAct  = Some(Left(eact)),
      payReqs   = None, // TODO Нужно сохранить сюда распарсенные платёжные атрибуты
      payReqsRaw = payReqs
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
  private def advJoinFormM = {
    Form(
      mapping(
        companyKM,
        townKM,
        "info"      -> text2048M
          .transform[Option[String]]({str => emptyStrOptToNone(Option(str))}, strOptGetOrElseEmpty),
        "address"   -> addressM,
        "siteUrl"   -> urlStrOptM,
        "phone"     -> phoneM,
        "email"     -> email,
        colorOptKM,
        logoKM,
        CAPTCHA_ID_FN    -> Captcha.captchaIdM,
        CAPTCHA_TYPED_FN -> Captcha.captchaTypedM
      )
      {(companyName, town, info, address, siteUrl, phone, email1, color, logoOpt, _, _) =>
        val mir = applyForm(companyName = companyName, address = address, siteUrl = siteUrl, town = town,
          phone = phone, email1 = email1, anmt = AdNetMemberTypes.SHOP, withMmp = false,
          reqType = InviteReqTypes.Adv, info = info, color = color)
        (mir, logoOpt)
      }
      {case (mir, logoOpt) =>
        val companyName = unapplyCompanyName(mir)
        val town = unapplyTown(mir)
        val info = unapplyInfo(mir)
        val address = unapplyAddress(mir)
        val siteUrl = unapplySiteUrl(mir)
        val officePhone = unapplyOfficePhone(mir)
        val email1 = unapplyEmail(mir)
        val color = unapplyColor(mir)
        Some((companyName, town, info, address, siteUrl, officePhone, email1, color, logoOpt, "", ""))
      }
    )
  }


  /** Юзер хочется зарегаться как рекламное агентство. Отрендерить страницу с формой, похожей на форму
    * заполнения сведений по wi-fi сети. */
  def joinAdvRequest = MaybeAuth { implicit request =>
    cacheControlShort {
      Ok(joinAdvTpl(advJoinFormM))
    }
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
      {case (mir, logoOpt) =>
        assert(mir.adnNode.exists(_.isLeft), "error.mir.adnNode.not.isLeft")
        val savedLogoFut = ImgFormUtil.updateOrigImgFull(logoOpt.toSeq, oldImgs = Nil)
          .flatMap { vs => ImgFormUtil.optImg2OptImgInfo(vs.headOption) }
        savedLogoFut flatMap { savedLogoOpt =>
          val mir2 = mir.copy(
            adnNode = mir.adnNode.map {
              _.left.map { adnNode0 =>
                adnNode0.copy(
                  logoImgOpt = savedLogoOpt
                )
              }
            }
          )
          mir2.save.map { irId =>
            sendEmailNewIR(irId, mir2)
            rmCaptcha(formBinded) {
              mirSavedRdr(irId)
            }
          }
        }
      }
    )
  }


  /** Отправить письмецо администрации s.io с ссылой на созданный запрос. */
  private def sendEmailNewIR(irId: String, mir0: MInviteRequest)(implicit request: RequestHeader) {
    val suEmailsConfKey = "market.join.request.notify.superusers.emails"
    val emails: Seq[String] = configuration.getStringSeq(suEmailsConfKey).getOrElse {
      // Нет ключа, уведомить разработчика, чтобы он настроил конфиг.
      warn(s"""I don't know, whom to notify about new invite request. Add following setting into your application.conf:\n  $suEmailsConfKey = ["support@sugest.io"]""")
      Seq("support@suggest.io")
    }
    val msg = MailerWrapper.instance
    msg.setRecipients(emails : _*)
    msg.setFrom("no-reply@suggest.io")
    msg.setSubject("Новый запрос на подключение | Suggest.io")
    val ctx = ContextImpl()
    val mir1 = mir0.copy(id = Some(irId))
    msg.setText( views.txt.sys1.market.invreq.emailNewIRCreatedTpl(mir1)(ctx) )
    msg.send()
  }


  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  override def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT] = {
    Enumerator(
      routes.MarketJoin.joinAdvRequest()
    ) map { call =>
      SiteMapUrl(
        loc = ctx.SC_URL_PREFIX + call.url,
        lastMod = Some( PROJECT_CODE_LAST_MODIFIED.toLocalDate ),
        changeFreq = Some( ChangeFreqs.weekly )
      )
    }
  }

}
