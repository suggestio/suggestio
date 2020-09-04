package io.suggest.lk.c

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.kv.MKvStorage
import io.suggest.lk.m.{LoginSessionRestore, LoginSessionSaved, LoginSessionSet, MLoginSessionS}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.cookie.{HttpCookieUtil, MHttpCookie, MHttpCookieParsed}
import io.suggest.sec.SessionConst
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.text.StringUtil
import japgolly.univeq._

import scala.util.Try
import scalaz.syntax.id._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.2020 18:33
  * Description: Контроллер логин-токена.
  */
class LoginSessionAh[M](
                         modelRW: ModelRW[M, MLoginSessionS]
                       )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  private def KV_STORAGE_TOKEN_KEY = "sc.login.session.cookie"

  private def _storageAvail[T](token0: Pot[T])(ifAvailable: => Pot[T]): Pot[T] = {
    Try {
      if (MKvStorage.isAvailable) ifAvailable
      else token0.fail( new UnsupportedOperationException )
    }
      .fold(token0.fail, identity)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запустить выставление/сброс токена сессии.
    case m: LoginSessionSet =>
      val v0 = value

      val isStore = HttpCookieUtil.isOkOrDiscardCookie( m.cookie, receivedAt = None )
      val isSaveNew = isStore contains true

      val pot1 = if (isStore contains false)
        Pot.empty[MHttpCookieParsed]
      else
        v0.cookie ready m.cookie

      val fxOpt = Option.when( isSaveNew ) {
        Effect.action {
          val mkvs = MKvStorage(
            key   = KV_STORAGE_TOKEN_KEY,
            value = MHttpCookie(
              setCookieHeaderValue = m.cookie.toSetCookie,
            ),
          )
          MKvStorage.save( mkvs )
          LoginSessionSaved( pot1 )
        }
      }
        .orElse {
          Option.when( /*isDeleteOld? = !isSaveNew && */ v0.cookie.nonEmpty ) {
            Effect.action {
              MKvStorage.delete( KV_STORAGE_TOKEN_KEY )
              val pot3 = if (isStore.isEmpty)
                pot1
              else
                Pot.empty
              LoginSessionSaved( pot3 )
            }
          }
        }

      val v2 = (MLoginSessionS.token set pot1.pending() )(v0)
      ah.updatedSilentMaybeEffect( v2, fxOpt )


    // Сигнал о сохранении сессии логина в постоянное хранилище.
    case m: LoginSessionSaved =>
      val v0 = value
      val v2 = (MLoginSessionS.token set m.cookiePot)(v0)
      // TODO Надо какой-то таймер организовать, наверное.
      //      Чтобы перевалидировать кукис через определённое время, возможно делаяя keepalive для обновления сессии.
      //      А пока - сервер проверит и порешит.
      updatedSilent(v2)


    // Восстановить состояние токена из хранилища.
    case LoginSessionRestore =>
      val v0 = value

      val fx = Effect.action {
        val pot2 = _storageAvail(v0.cookie) {
          MKvStorage
            // Чтение из хранилища...
            .get[MHttpCookie]( KV_STORAGE_TOKEN_KEY )
            .fold( Pot.empty[MHttpCookieParsed] ) { mkvs =>
              val setCookieRaw = mkvs.value.setCookieHeaderValue
              def setCookieRaw32 = if (scalajs.LinkingInfo.developmentMode) setCookieRaw else StringUtil.strLimitLen(setCookieRaw, 32)
              HttpCookieUtil
                .parseCookies( setCookieRaw )
                // Убедится, что не пустая строка на выходе пропарсилась:
                .filterOrElse( _.nonEmpty, {
                  val e = ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT
                  logger.error( ErrorMsgs.COOKIE_NOT_PARSED, msg = (e, setCookieRaw32) )
                  e
                })
                // Найти сессионный кукис среди распарсенных (по идее тут только один сессионный кукис и есть.)
                .flatMap { cookiesParsed =>
                  val sesCooName = SessionConst.SESSION_COOKIE_NAME
                  cookiesParsed
                    .find(_.name ==* sesCooName)
                    .toRight {
                      val e = ErrorMsgs.NODE_NOT_FOUND
                      logger.error( ErrorMsgs.COOKIE_NOT_PARSED, msg = (sesCooName, e, setCookieRaw32) )
                      e
                    }
                }
                // Запретить использовать кукис, если он уже истёк.
                .flatMap { cookieParsed =>
                  HttpCookieUtil
                    .isOkOrDiscardCookie(
                      cookieParsed = cookieParsed,
                      receivedAt = mkvs.value.receivedAt into Some.apply
                    )
                    .collect {
                      case true => cookieParsed
                    }
                    .toRight {
                      val e = ErrorMsgs.EXPIRED_TOKEN
                      logger.info( e, msg = (cookieParsed.name, cookieParsed.expires, cookieParsed.maxAge) )
                      e
                    }
                }
                // Итого: запилить кукис в состояние контроллера, или удалить из хранилища.
                .fold [Pot[MHttpCookieParsed]] (
                  {error =>
                    logger.warn( ErrorMsgs.COOKIE_NOT_PARSED, msg = (KV_STORAGE_TOKEN_KEY, error, setCookieRaw32) )

                    // Эффект: Удалить некорректный кукис из хранилища:
                    MKvStorage.delete( KV_STORAGE_TOKEN_KEY )

                    Pot.empty
                  },
                  v0.cookie.ready
                )
            }
        }
        LoginSessionSaved( pot2 )
      }

      val v2 = MLoginSessionS.token.modify(_.pending())(v0)
      updatedSilent(v2, fx)

  }

}
