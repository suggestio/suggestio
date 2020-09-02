package io.suggest.lk.c

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.kv.MKvStorage
import io.suggest.lk.m.{LoginSessionRestore, LoginSessionSaved, LoginSessionSet, MLoginSessionS}
import io.suggest.proto.http.model.MHttpCookie
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.Try

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
{

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
      println(m)
      val v0 = value

      val pot2 = m.token
        .fold( Pot.empty[MHttpCookie] )( v0.cookie.ready )

      val fx = Effect.action {
        val pot3 = _storageAvail(v0.cookie) {
          val key = KV_STORAGE_TOKEN_KEY
          m.token.fold {
            MKvStorage.delete( key )
          } { token2 =>
            val mkvs = MKvStorage(
              key   = KV_STORAGE_TOKEN_KEY,
              value = token2,
            )
            MKvStorage.save( mkvs )
          }
          pot2
        }
        LoginSessionSaved( pot3 )
      }

      val v2 = MLoginSessionS.token.set {
        pot2.pending()
      }(v0)
      updatedSilent(v2, fx)


    // Сигнал о сохранении сессии логина в постоянное хранилище.
    case m: LoginSessionSaved =>
      val v0 = value
      val v2 = (MLoginSessionS.token set m.res)(v0)
      updatedSilent(v2)


    // Восстановить состояние токена из хранилища.
    case LoginSessionRestore =>
      val v0 = value
      val fx = Effect.action {
        val pot2 = _storageAvail(v0.cookie) {
          MKvStorage
            .get[MHttpCookie]( KV_STORAGE_TOKEN_KEY )
            .fold( Pot.empty[MHttpCookie] ) { mkvs =>
              v0.cookie.ready( mkvs.value )
            }
        }
        LoginSessionSaved( pot2 )
      }
      val v2 = MLoginSessionS.token.modify(_.pending())(v0)
      updatedSilent(v2, fx)

  }

}
