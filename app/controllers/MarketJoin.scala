package controllers

import com.google.inject.Inject
import models.CallBackReqCallTimes.CallBackReqCallTime
import play.api.i18n.MessagesApi
import util.captcha.CaptchaUtil._
import util.PlayMacroLogsImpl
import util.acl.{MaybeAuthPost, MaybeAuthGet}
import util.SiowebEsUtil.client
import models._
import util.mail.IMailerWrapper
import views.html.market.join._
import util.FormUtil._
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}
import play.api.mvc.RequestHeader

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.06.14 18:29
 * Description: Контроллер раздела сайта со страницами и формами присоединения к sio-market.
 */
class MarketJoin @Inject() (
  override val messagesApi: MessagesApi,
  override val mailer: IMailerWrapper
)
  extends SioController with PlayMacroLogsImpl with CaptchaValidator with IMailer
{

  import LOGGER._

  /** Маппинг формы запроса обратного звонка с капчей, именем, телефоном и временем прозвона. */
  private def callbackRequestFormM: Form[MInviteRequest] = {
    Form(
      mapping(
        "name"  -> nameM,
        "phone" -> phoneM,
        "callTime" -> CallBackReqCallTimes.mapping,
        CAPTCHA_ID_FN     -> captchaIdM,
        CAPTCHA_TYPED_FN  -> captchaTypedM
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
  def callbackRequest = MaybeAuthGet { implicit request =>
    cacheControlShort {
      Ok(callbackRequestTpl(callbackRequestFormM))
    }
  }

  /**
   * Сабмит формы запроса обратного вызова.
   * @return
   */
  def callbackRequestSubmit = MaybeAuthPost.async { implicit request =>
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


  private def unapplyCompanyName(mir: MInviteRequest): String = {
    mir.company
      .left.map(_.meta.name)
      .left.getOrElse {
        mir.adnNode
          .fold("") { _.left.map(_.meta.name).left.getOrElse("") }
      }
  }

  private def unapplyOfficePhone(mir: MInviteRequest): String = {
    mir.company
      .left.map(_.meta.officePhones.headOption)
      .left.getOrElse(None)
      .getOrElse("")
  }


  /** Юзер хочется зарегаться как рекламное агентство. Отрендерить страницу с формой, похожей на форму
    * заполнения сведений по wi-fi сети. */
  def joinAdvRequest = MaybeAuthGet { implicit request =>
    cacheControlShort {
      Ok(joinAdvTpl())
    }
  }


  /** Отправить письмецо администрации s.io с ссылой на созданный запрос. */
  private def sendEmailNewIR(irId: String, mir0: MInviteRequest)(implicit request: RequestHeader) {
    val suEmailsConfKey = "market.join.request.notify.superusers.emails"
    val emails: Seq[String] = configuration.getStringSeq(suEmailsConfKey).getOrElse {
      // Нет ключа, уведомить разработчика, чтобы он настроил конфиг.
      warn(s"""I don't know, whom to notify about new invite request. Add following setting into your application.conf:\n  $suEmailsConfKey = ["support@sugest.io"]""")
      Seq("support@suggest.io")
    }
    val msg = mailer.instance
    msg.setRecipients(emails : _*)
    msg.setFrom("no-reply@suggest.io")
    msg.setSubject("Новый запрос на подключение | Suggest.io")
    val ctx = ContextImpl()
    val mir1 = mir0.copy(id = Some(irId))
    msg.setText( views.txt.sys1.market.invreq.emailNewIRCreatedTpl(mir1)(ctx) )
    msg.send()
  }

}
