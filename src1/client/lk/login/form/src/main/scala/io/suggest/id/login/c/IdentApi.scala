package io.suggest.id.login.c

import io.suggest.captcha.MCaptchaCheckReq
import io.suggest.err.MCheckException
import io.suggest.id.login.MEpwLoginReq
import io.suggest.id.pwch.MPwChangeForm
import io.suggest.id.reg.{MCodeFormReq, MRegCreds0, MRegTokenResp}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpReq, HttpReqData}
import io.suggest.routes.{PlayRoute, routes}
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.common.fut.FutureUtil.Implicits._
import play.api.libs.json.{Json, Writes}
import japgolly.univeq._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 13:02
  * Description: API доступа к серверу.
  */
trait IIdentApi {

  /** Сабмит формы логина-пароля.
    *
    * @param loginReq Данные логина по паролю.
    * @param r Обратный (возвращаемый) редирект при положительном исходе.
    * @return Фьючерс с результатом.
    */
  def epw2LoginSubmit(loginReq: MEpwLoginReq, r: Option[String] = None): Future[String]

  /** Сабмит реквизитов первого шага.
    *
    * @param creds0 Реквизиты регистрации 0-шага.
    * @return Токен для получения капчи.
    */
  def regStep0Submit(creds0: MRegCreds0): Future[MRegTokenResp]

  /** Сабмит формы регистрации на сервер.
    *
    * @param form Данные формы, введённые юзером.
    * @return Фьючерс с ответом сервер.
    */
  def epw2RegSubmit(form: MCaptchaCheckReq): Future[MRegTokenResp]

  /** Сабмит проверки смс-кода.
    *
    * @param req Реквест, содержащий данные смс-кода и токен.
    * @return Ответ с обновлённым токеном.
    */
  def smsCodeCheck(req: MCodeFormReq): Future[MRegTokenResp]

  /** Сабмит галочек и нового пароля.
    *
    * @param req Реквест с указанным паролем.
    * @return Ответ по регистрации.
    */
  def regFinalSubmit(req: MCodeFormReq): Future[MRegTokenResp]


  /** Сабмит формы смена пароля.
    *
    * @param form Данные формы смены пароля.
    * @return Фьючерс, когда всё ок.
    */
  def pwChangeSubmit(form: MPwChangeForm): Future[None.type]

}


class IdentApiHttp extends IIdentApi {

  private def _timeoutMsSome = Some( 10.seconds.toMillis.toInt )

  override def epw2LoginSubmit(loginReq: MEpwLoginReq, r: Option[String] = None): Future[String] = {
    for {
      resp <- HttpClient.execute(
        HttpReq.routed(
          route = routes.controllers.Ident.epw2LoginSubmit( r.toUndef ),
          data  = HttpReqData(
            headers = {
              val H = HttpConst.Headers
              Map(
                H.CONTENT_TYPE -> MimeConst.APPLICATION_JSON,
                H.ACCEPT       -> MimeConst.TEXT_PLAIN,
              )
            },
            body = Json
              .toJson( loginReq )
              .toString(),
            timeoutMs = _timeoutMsSome,
          )
        )
      )
        .resultFut
        .successIf200
      rdrUrl <- resp.text()
    } yield {
      rdrUrl
    }
  }


  /** Общий код для сабмитов за tokenReq с сервера.
    *
    * @param data Данные тела запроса.
    * @param route Роута.
    * @tparam A Тип тела запроса.
    * @return Фьючерс с token resp.
    */
  private def _tokenReq[A: Writes](data: A, route: PlayRoute): Future[MRegTokenResp] = {
    HttpClient.execute(
      HttpReq.routed(
        route = route,
        data = HttpReqData(
          headers = HttpReqData.headersJsonSendAccept,
          body = Json
            .toJson( data )
            .toString(),
          timeoutMs = _timeoutMsSome
        )
      )
    )
      .resultFut
      // И распарсить ответ:
      .unJson[MRegTokenResp]
  }

  override def regStep0Submit(creds0: MRegCreds0): Future[MRegTokenResp] =
    _tokenReq( creds0, routes.controllers.Ident.regStep0Submit() )

  override def epw2RegSubmit(form: MCaptchaCheckReq): Future[MRegTokenResp] =
    _tokenReq( form, routes.controllers.Ident.epw2RegSubmit() )

  override def smsCodeCheck(req: MCodeFormReq): Future[MRegTokenResp] =
    _tokenReq( req, routes.controllers.Ident.smsCodeCheck() )

  override def regFinalSubmit(req: MCodeFormReq): Future[MRegTokenResp] =
    _tokenReq( req, routes.controllers.Ident.regFinalSubmit() )


  override def pwChangeSubmit(form: MPwChangeForm): Future[None.type] = {
    val httpReq = HttpReq.routed(
      route = routes.controllers.Ident.pwChangeSubmit(),
      data  = HttpReqData(
        headers = HttpReqData.headersJsonSendAccept,
        body = Json
          .toJson( form )
          .toString(),
        timeoutMs = _timeoutMsSome,
      )
    )
    HttpClient
      .execute( httpReq )
      .resultFut
      .flatMap { httpResp =>
        // Разобрать ответ сервера: пароль сохранён | ошибка клиента | ошибка сервера
        if (httpResp.status ==* HttpConst.Status.NO_CONTENT) {
          Future.successful( None )
        } else if (httpResp.status ==* HttpConst.Status.NOT_ACCEPTABLE) {
          httpResp
            .unJson[MCheckException]
            .toFutureFailed
        } else {
          httpResp
            .toHttpFailedException( httpReq )
        }
      }
  }

}
