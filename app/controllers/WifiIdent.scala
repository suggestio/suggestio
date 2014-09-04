package controllers

import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.acl.MaybeAuth
import util.radius.RadiusServerImpl
import views.html.ident.wifi._
import play.api.data._, Forms._
import util.FormUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 13:31
 * Description: Ident-контроллер для идентификации пользователей, хотящих в халявный wifi.
 */
object WifiIdent extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  val smsCode = "12345"

  private def idByPhoneFormM = Form(
    "phone" -> phoneM
  )

  /** Рендер страницы с формой ввода номера телефона. */
  def idByPhone = MaybeAuth { implicit request =>
    Ok(idByPhoneTpl(idByPhoneFormM))
  }

  def idByPhoneSubmit = MaybeAuth { implicit request =>
    idByPhoneFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("idByPhoneSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(idByPhoneTpl(formWithErrors))
      },
      {phone =>
        // TODO Нужно выслать смс с кодом подтверждения на указанный номер
        // TODO Нужно сохранить временные данные о номере и коде куда-то в инфу.
        Redirect( routes.WifiIdent.idByPhoneCode(phone) )
          .flashing("success" -> "На ваш номер отправлено смс с кодом подтверждения (TODO)")
      }
    )
  }


  private def idByPhoneCodeFormM = Form(
    "code" -> nonEmptyText(minLength = 2, maxLength = 16)
  )

  /** Рендер страницы с формой ввода кода подтверждения, присланного по смс. */
  // TODO Проверять телефон на валидность?
  def idByPhoneCode(phone: String) = MaybeAuth { implicit request =>
    Ok(idByPhoneCodeTpl(phone, idByPhoneCodeFormM))
  }


  def idByPhoneCodeSubmit(phone: String) = MaybeAuth { implicit request =>
    lazy val logPrefix = s"idByPhoneCodeSubmit($phone): "
    val formBinded = idByPhoneCodeFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(idByPhoneCodeTpl(phone, formWithErrors))
      },
      { code =>
        // TODO Проверить валидность кода.
        val isCodeValid = code equalsIgnoreCase smsCode
        if (isCodeValid) {
          // Код валиден. Нужно чтобы радиус-сервер узнал об этом.
          RadiusServerImpl.addPhone(phone, code)
          Ok(idByPhoneOkTpl(
            username = phone,
            password = code,
            submitUrl = "http://192.168.77.1/login"
          ))

        } else {
          // Код подтверждения не верен. Вернуть страницу с формой юзеру.
          debug(logPrefix + "Invalid code: " + code)
          val formWithErrors = formBinded.withError("code", "error.code.invalid")
          NotAcceptable(idByPhoneCodeTpl(phone, formWithErrors))
        }
      }
    )
  }

}
