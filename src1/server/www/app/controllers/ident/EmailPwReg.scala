package controllers.ident

import controllers.{CaptchaValidator, SioController, routes}
import io.suggest.ctx.CtxData
import io.suggest.i18n.MsgCodes
import io.suggest.init.routed.MJsInitTargets
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.sec.m.msession.Keys
import io.suggest.sec.util.IScryptUtilDi
import io.suggest.util.logs.IMacroLogs
import models.{EmailPwConfirmForm_t, EmailPwRegReqForm_t}
import models.mctx.Context
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
import util.di.IIdentUtil
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
  with IIsAnonAcl
  with IIdentUtil
  with INodesUtil
  with EmailPwRegUtil
  with IMNodes
  with IMPersonIdents
  with IEmailPwIdentsDi
  with IEmailActivationsDi
  with IScryptUtilDi
{

  import mCommonDi._

  val canConfirmEmailPwReg: CanConfirmEmailPwReg

  def sendEmailAct(ea: EmailActivation)(implicit ctx: Context): Future[_] = {
    val msg = mailer.instance
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
      jsInitTargets = MJsInitTargets.CaptchaForm :: Nil
    )
    epwRegTpl(form, captchaShown = true)
  }

  /**
    * Страница с колонкой регистрации по email'у.
    *
    * @return 200 OK со страницей начала регистрации по email.
    */
  def emailReg = csrf.AddToken {
    isAnon() { implicit request =>
      Ok(_epwRender(emailRegFormM))
    }
  }

  /**
    * Сабмит формы регистрации по email.
    * Нужно отправить письмо на указанный ящик и отредиректить юзера на страницу с инфой.
    *
    * @return emailRegFormBindFailed() при проблеме с маппингом формы.
    *         emailRequestOk() когда сообщение отправлено почтой.
    */
  def emailRegSubmit = csrf.Check {
    isAnon().async { implicit request =>
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
                key   = CanConfirmEmailPwReg.EPW_ACT_KEY
              )
              for {
                eaId <- emailActivations.save(ea0)
                // отправить письмо на указанную почту
                ea1 = ea0.copy(id = Some(eaId))
                _ <- sendEmailAct(ea1)
              } yield {
                // Вернуть ответ юзеру
                emailRequestOk(Some(ea1))
              }

            // Уже есть такой email в базе. Выслать восстановление пароля.
            case _ =>
              LOGGER.error(s"emailRegSubmit($email1): Email already exists.")
              for {
                _ <- sendRecoverMail(email1)
              } yield {
                emailRequestOk(None)
              }
          }
        }
      )
    }
  }

  /** Что возвращать юзеру, когда сообщение отправлено на почту? */
  protected def emailRequestOk(ea: Option[EmailActivation])(implicit ctx: Context): Result = {
    Ok(sentTpl(ea)(ctx))
  }

  private def _eaNotFound(req: IReq[_]): Future[Result] = {
    val rdrFut = req.user.personIdOpt.fold[Future[Result]] {
      Redirect( routes.Ident.emailPwLoginForm() )
    } { personId =>
      identUtil.redirectUserSomewhere(personId)
    }
    // TODO Отправлять на страницу, где описание проблема, а не туда, куда взбредёт.
    for (rdr <- rdrFut) yield {
      rdr.flashing(FLASH.ERROR -> MsgCodes.`Activation.impossible`)
    }
  }


  /** Юзер возвращается по ссылке из письма. Отрендерить страницу завершения регистрации. */
  def emailReturn(eaInfo: IEaEmailId) = csrf.AddToken {
    canConfirmEmailPwReg(eaInfo)(_eaNotFound) { implicit request =>
      // ActionBuilder уже выверил всё. Нужно показать юзеру страницу с формой ввода пароля, названия узла и т.д.
      Ok(confirmTpl(request.eact, epwRegConfirmFormM))
    }
  }


  /** Сабмит формы подтверждения регистрации по email. */
  def emailConfirmSubmit(eaInfo: IEaEmailId) = csrf.Check {
    canConfirmEmailPwReg(eaInfo)(_eaNotFound).async { implicit request =>
      // ActionBuilder выверил данные из письма, надо забиндить данные регистрации, создать узел и т.д.
      epwRegConfirmFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"emailConfirmSubmit($eaInfo): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          NotAcceptable(confirmTpl(request.eact, formWithErrors))
        },
        {data =>
          implicit val ctx = implicitly[Context]
          // Создать юзера и его ident, удалить активацию, создать новый узел-ресивер.
          val lang = ctx.messages.lang
          val mperson0 = MNode(
            common = MNodeCommon(
              ntype = MNodeTypes.Person,
              isDependent = false
            ),
            meta = MMeta(
              basic = MBasicMeta(
                nameOpt = Some(eaInfo.email),
                langs   = lang.code :: Nil
              ),
              person = MPersonMeta(
                emails = eaInfo.email :: Nil
              )
            )
          )

          for {
          // Сохранить узел самого юзера.
            personId <- mNodes.save(mperson0)

            // Развернуть узел-магазин для юзера
            mnodeFut = nodesUtil.createUserNode(name = data.adnName, personId = personId)

            // Запустить сохранение ident'а юзера, дождаться сохранения.
            _ <- {
              val epw0 = EmailPwIdent(
                email       = eaInfo.email,
                personId    = personId,
                pwHash      = scryptUtil.mkHash(data.password),
                isVerified  = true
              )
              emailPwIdents.save(epw0)
            }

            // Удалить email activation
            _ <- emailActivations.deleteById( request.eact.id.get )

            // Дождаться готовности магазина юзера
            mnode <- mnodeFut

          } yield {
            val args = nodesUtil.nodeRegSuccessArgs(mnode)
            Ok( regSuccessTpl(args)(ctx) )
              .addingToSession(Keys.PersonId.value -> personId)
              .withLang(lang)
          }
        } // Form.fold right
      )   // Form.fold
    }
  }

}
