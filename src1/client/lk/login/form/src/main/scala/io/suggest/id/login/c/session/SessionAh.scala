package io.suggest.id.login.c.session

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.IdentConst.CookieToken.KV_STORAGE_TOKEN_KEY
import io.suggest.id.login.m.session.MSessionS
import io.suggest.kv.MKvStorage
import io.suggest.lk.m.{SessionRestore, SessionSaved, SessionSet}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.cookie.{HttpCookieUtil, MCookieState, MHttpCookie}
import io.suggest.sec.SessionConst
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.text.StringUtil
import japgolly.univeq._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.2020 18:33
  * Description: Контроллер JWT-токена сессии, который между клиентом и сервером передаётся через кукис.
  *
  * Кукис сессии обновляется не только при логине, но и просто с ходом времени,
  * с CSRF-GET-запросами и при прочих ситуациях.
  */
class SessionAh[M](
                    modelRW: ModelRW[M, MSessionS],
                  )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  private def _storageAvail[T](token0: Pot[T])(ifAvailable: => Pot[T]): Pot[T] = {
    Try {
      if (MKvStorage.isAvailable) ifAvailable
      else token0.fail( new UnsupportedOperationException )
    }
      .fold(token0.fail, identity)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запустить выставление/сброс токена сессии.
    case m: SessionSet =>
      val v0 = value

      val isStore = HttpCookieUtil.isOkOrDiscardCookie(
        m.cookie.parsed,
        receivedAt = Some( m.cookie.meta.receivedAt ),
      )
      val isSaveNew = isStore contains[Boolean] true

      val pot1 = if (isStore contains[Boolean] false)
        Pot.empty[MCookieState]
      else
        v0.cookie ready m.cookie

      val fxOpt = Option.when( isSaveNew ) {
        Effect.action {
          val mkvs = MKvStorage(
            key   = KV_STORAGE_TOKEN_KEY,
            value = m.cookie.toRawCookie,
          )
          MKvStorage.save( mkvs )
          SessionSaved( pot1 )
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
              SessionSaved( pot3 )
            }
          }
        }

      val v2 = (MSessionS.token set pot1.pending() )(v0)
      ah.updatedSilentMaybeEffect( v2, fxOpt )


    // Сигнал о сохранении сессии логина в постоянное хранилище.
    case m: SessionSaved =>
      val v0 = value
      val v2 = (MSessionS.token set m.cookiePot)(v0)
      // TODO Надо какой-то таймер организовать, наверное.
      //      Чтобы перевалидировать кукис через определённое время, возможно делаяя keepalive для обновления сессии.
      //      А пока - сервер проверит и порешит.
      updated(v2)


    // Восстановить состояние токена из хранилища.
    case SessionRestore =>
      val v0 = value

      val fx = Effect.action {
        val pot2 = _storageAvail(v0.cookie) {
          MKvStorage
            // Чтение из хранилища...
            .get[MHttpCookie]( KV_STORAGE_TOKEN_KEY )
            .fold( Pot.empty[MCookieState] ) { mkvs =>
              val cookie = mkvs.value

              def setCookieRaw32 =
                if (scalajs.LinkingInfo.developmentMode) cookie.setCookieHeaderValue
                else StringUtil.strLimitLen(cookie.setCookieHeaderValue, 32)

              HttpCookieUtil
                .parseCookies( cookie.setCookieHeaderValue )
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
                      receivedAt   = Some( cookie.meta.receivedAt ),
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
                .fold [Pot[MCookieState]] (
                  {error =>
                    logger.warn( ErrorMsgs.COOKIE_NOT_PARSED, msg = (KV_STORAGE_TOKEN_KEY, error, setCookieRaw32) )

                    // Эффект: Удалить некорректный кукис из хранилища:
                    MKvStorage.delete( KV_STORAGE_TOKEN_KEY )

                    Pot.empty
                  },
                  { cookieParsed =>
                    val cookieState = MCookieState(
                      parsed  = cookieParsed,
                      meta    = cookie.meta,
                    )
                    v0.cookie ready cookieState
                  }
                )
            }
        }
        SessionSaved( pot2 )
      }

      val v2 = MSessionS.token.modify(_.pending())(v0)
      updated(v2, fx)

  }

}
