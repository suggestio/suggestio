package controllers.ident

import controllers.{CaptchaValidator, SioController, routes}
import io.suggest.ctx.CtxData
import io.suggest.es.model.EsModelDi
import io.suggest.i18n.MsgCodes
import io.suggest.init.routed.MJsInitTargets
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sec.util.IScryptUtilDi
import io.suggest.session.MSessionKeys
import io.suggest.util.logs.IMacroLogs
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
import util.ident.IIdentUtil
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
      "email"      -> email,
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
  with IScryptUtilDi
  with EsModelDi
{

  import mCommonDi._
  import esModel.api._

  val canConfirmEmailPwReg: CanConfirmEmailPwReg

  def sendEmailAct(qs: MEmailRecoverQs)(implicit ctx: Context): Future[_] = {
    val msg = mailer.instance
    msg.setRecipients(qs.email)
    msg.setSubject("Suggest.io | " + ctx.messages("reg.emailpw.email.subj"))
    msg.setHtml {
      htmlCompressUtil.html4email {
        emailRegMsgTpl(qs)(ctx)
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
        // Всё ок
        {email1 =>
          // Почта уже зарегана может?
          mNodes
            .dynExists {
              new MNodeSearchDfltImpl {
                override val nodeTypes = MNodeTypes.Person :: Nil
                override val outEdges: Seq[Criteria] = {
                  val cr = Criteria(
                    predicates  = MPredicates.Ident.Email :: Nil,
                    nodeIds     = email1 :: Nil,
                  )
                  cr :: Nil
                }
              }
            }
            .flatMap {
              // Нет такого email. Отправить письмо активации.
              case false =>
                val qs = MEmailRecoverQs(
                  email = email1
                )
                for {
                  _ <- sendEmailAct(qs)
                } yield {
                  // Вернуть ответ юзеру
                  emailRequestOk(Some(qs))
                }

              // Уже есть такой email в базе. Выслать восстановление пароля.
              case true =>
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
  protected def emailRequestOk(qs: Option[MEmailRecoverQs])(implicit ctx: Context): Result = {
    Ok(sentTpl(qs)(ctx))
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
  def emailReturn(qs: MEmailRecoverQs) = csrf.AddToken {
    canConfirmEmailPwReg(qs)(_eaNotFound) { implicit request =>
      // ActionBuilder уже выверил всё. Нужно показать юзеру страницу с формой ввода пароля, названия узла и т.д.
      Ok(confirmTpl(qs, epwRegConfirmFormM))
    }
  }


  /** Сабмит формы подтверждения регистрации по email. */
  def emailConfirmSubmit(qs: MEmailRecoverQs) = csrf.Check {
    canConfirmEmailPwReg(qs)(_eaNotFound).async { implicit request =>
      // ActionBuilder выверил данные из письма, надо забиндить данные регистрации, создать узел и т.д.
      epwRegConfirmFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"emailConfirmSubmit(${qs.email}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          NotAcceptable(confirmTpl(qs, formWithErrors))
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
                nameOpt = Some(qs.email),
                langs   = lang.code :: Nil
              ),
              person = MPersonMeta(
                emails = qs.email :: Nil
              )
            ),
            edges = MNodeEdges(
              out = {
                MNodeEdges.edgesToMap(
                  // Пароль
                  MEdge(
                    predicate = MPredicates.Ident.Password,
                    info = MEdgeInfo(
                      textNi = Some( scryptUtil.mkHash(data.password) )
                    )
                  ),
                  // Email
                  MEdge(
                    predicate = MPredicates.Ident.Email,
                    nodeIds = Set( qs.email ),
                    info = MEdgeInfo(
                      flag = Some(true)
                    )
                  )
                )
              }
            )
          )

          for {
            // Сохранить узел юзера.
            personId <- mNodes.save( mperson0 )

            // Развернуть узел-магазин для юзера
            mnode <- nodesUtil.createUserNode(name = data.adnName, personId = personId)

          } yield {
            val args = nodesUtil.nodeRegSuccessArgs(mnode)
            Ok( regSuccessTpl(args)(ctx) )
              .addingToSession(MSessionKeys.PersonId.value -> personId)
              .withLang(lang)
          }
        } // Form.fold right
      )   // Form.fold
    }
  }

}
