package controllers.ident

import controllers.{CaptchaValidator, SioController}
import io.suggest.model.n2.node.IMNodes
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.util.logs.IMacroLogs
import models._
import models.jsm.init.MTargets
import models.mctx.{Context, CtxData}
import models.msession.Keys
import models.req.IReq
import models.usr._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Result
import play.twirl.api.Html
import util.captcha.CaptchaUtil._
import util.captcha.ICaptchaUtilDi
import util.mail.IMailerWrapperDi
import util.FormUtil
import util.acl._
import util.adn.INodesUtil
import util.secure.IScryptUtilDi
import views.html.ident.reg.regSuccessTpl
import views.html.ident.reg.email._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 18:04
 * Description: Поддержка регистрации по имени и паролю в контроллере.
 */
trait EmailPwRegUtil extends ICaptchaUtilDi {

  /** Маппинг формы регистрации по email. Форма с капчей. */
  def emailRegFormM: EmailPwRegReqForm_t = Form(
    mapping(
      "email"           -> email,
      CAPTCHA_ID_FN     -> captchaUtil.captchaIdM,
      CAPTCHA_TYPED_FN  -> captchaUtil.captchaTypedM
    )
    {(email1, _, _) => email1 }
    {email1 => Some((email1, "", ""))}
  )

  /** Форма подтверждения регистрации по email и паролю. */
  def epwRegConfirmFormM: EmailPwConfirmForm_t = Form(
    mapping(
      "nodeName" -> FormUtil.nameM,
      "pw"       -> FormUtil.passwordWithConfirmM
    )
    { EmailPwConfirmInfo.apply }
    { EmailPwConfirmInfo.unapply }
  )

}


trait EmailPwReg
  extends SioController
  with IMacroLogs
  with CaptchaValidator
  with SendPwRecoverEmail
  with IMailerWrapperDi
  with IsAnon
  with CanConfirmEmailPwRegCtl
  with INodesUtil
  with EmailPwRegUtil
  with IMNodes
  with IMPersonIdents
  with IEmailPwIdentsDi
  with IEmailActivationsDi
  with IScryptUtilDi
{

  import mCommonDi._

  def sendEmailAct(ea: EmailActivation)(implicit ctx: Context): Unit = {
    val msg = mailer.instance
    msg.setFrom("no-reply@suggest.io")
    msg.setRecipients(ea.email)
    msg.setSubject("Suggest.io | " + ctx.messages("reg.emailpw.email.subj"))
    msg.setHtml {
      htmlCompressUtil.html4email {
        emailRegMsgTpl(ea)(ctx)
      }
    }
    msg.send()
  }

  /** Рендер страницы регистрации по email. */
  private def _epwRender(form: EmailPwRegReqForm_t)(implicit request: IReq[_]): Html = {
    implicit val ctxData = CtxData(
      jsiTgs = Seq(MTargets.CaptchaForm)
    )
    epwRegTpl(form, captchaShown = true)
  }

  /**
    * Страница с колонкой регистрации по email'у.
    *
    * @return 200 OK со страницей начала регистрации по email.
    */
  def emailReg = IsAnonGet { implicit request =>
    Ok(_epwRender(emailRegFormM))
  }

  /**
    * Сабмит формы регистрации по email.
    * Нужно отправить письмо на указанный ящик и отредиректить юзера на страницу с инфой.
    *
    * @return emailRegFormBindFailed() при проблеме с маппингом формы.
    *         emailRequestOk() когда сообщение отправлено почтой.
    */
  def emailRegSubmit = IsAnonPost.async { implicit request =>
    val form1 = checkCaptcha( emailRegFormM.bindFromRequest() )
    form1.fold(
      {formWithErrors =>
        LOGGER.debug("emailRegSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable( _epwRender(formWithErrors) )
      },
      {email1 =>
        // Почта уже зарегана может?
        mPersonIdents.findIdentsByEmail(email1).flatMap {
          // Нет такого email. Собираем активацию.
          case nil if nil.isEmpty =>    // Используем isEmpty во избежания скрытых изменений в API в будущем
            // Сохранить новый eact
            val ea0 = EmailActivation(
              email = email1,
              key = CanConfirmEmailPwReg.EPW_ACT_KEY
            )
            emailActivations.save(ea0)
              .flatMap { eaId =>
                // отправить письмо на указанную почту
                val ea1 = ea0.copy(id = Some(eaId))
                sendEmailAct(ea1)
                // Вернуть ответ юзеру
                emailRequestOk(Some(ea1))
              }

          // Уже есть такой email в базе. Выслать восстановление пароля.
          case idents =>
            LOGGER.error(s"emailRegSubmit($email1): Email already exists.")
            sendRecoverMail(email1) flatMap { _ =>
              emailRequestOk(None)
            }
        }
      }
    )
  }

  /** Что возвращать юзеру, когда сообщение отправлено на почту? */
  protected def emailRequestOk(ea: Option[EmailActivation])(implicit ctx: Context): Future[Result] = {
    Ok(sentTpl(ea)(ctx))
  }


  /** Юзер возвращается по ссылке из письма. Отрендерить страницу завершения регистрации. */
  def emailReturn(eaInfo: IEaEmailId) = CanConfirmEmailPwRegGet(eaInfo) { implicit request =>
    // ActionBuilder уже выверил всё. Нужно показать юзеру страницу с формой ввода пароля, названия узла и т.д.
    Ok(confirmTpl(request.eact, epwRegConfirmFormM))
  }

  /** Сабмит формы подтверждения регистрации по email. */
  def emailConfirmSubmit(eaInfo: IEaEmailId) = CanConfirmEmailPwRegPost(eaInfo).async { implicit request =>
    // ActionBuilder выверил данные из письма, надо забиндить данные регистрации, создать узел и т.д.
    epwRegConfirmFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"emailConfirmSubmit($eaInfo): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(confirmTpl(request.eact, formWithErrors))
      },
      {data =>
        // Создать юзера и его ident, удалить активацию, создать новый узел-ресивер.
        val lang = request2lang
        val mperson0 = MNode(
          common = MNodeCommon(
            ntype = MNodeTypes.Person,
            isDependent = false
          ),
          meta = MMeta(
            basic = MBasicMeta(
              nameOpt = Some(eaInfo.email),
              langs   = List( lang.code )
            ),
            person = MPersonMeta(
              emails = List(eaInfo.email)
            )
          )
        )

        for {
          // Сохранить узел самого юзера.
          personId <- mNodes.save(mperson0)

          // Развернуть узел-магазин для юзера
          mnodeFut = nodesUtil.createUserNode(name = data.adnName, personId = personId)

          // Запустить сохранение ident'а юзера.
          identIdFut = {
            val epw0 = EmailPwIdent(
              email       = eaInfo.email,
              personId    = personId,
              pwHash      = scryptUtil.mkHash(data.password),
              isVerified  = true
            )
            emailPwIdents.save(epw0)
          }

          // Дождаться ident'а
          identId <- identIdFut

          // Удалить email activation
          _ <- emailActivations.deleteById( request.eact.id.get )

          // Дождаться готовности магазина юзера
          mnode <- mnodeFut

        } yield {
          val args = nodesUtil.nodeRegSuccessArgs(mnode)
          Ok( regSuccessTpl(args) )
            .addingToSession(Keys.PersonId.name -> personId)
            .withLang(lang)
        }
      } // Form.fold right
    )   // Form.fold
  }

}
