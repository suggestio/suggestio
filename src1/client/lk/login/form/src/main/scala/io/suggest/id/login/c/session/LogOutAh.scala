package io.suggest.id.login.c.session

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.IdentConst
import io.suggest.id.login.c.IIdentApi
import io.suggest.id.login.m.{LoginFormDiConfig, LogoutConfirm, LogoutStep}
import io.suggest.id.login.m.session.MLogOutDia
import io.suggest.kv.MKvStorage
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.2020 22:17
  * Description: Контроллер logout-диалога.
  */
class LogOutAh[M](
                   modelRW    : ModelRW[M, Option[MLogOutDia]],
                   diConfig   : LoginFormDiConfig,
                   identApi   : IIdentApi,
                 )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Шаг logout-работы.
    case m: LogoutStep =>
      val v0 = value

      if (m.pot ==* Pot.empty) {
        // Нулевой шал logout: Запрос открытия logout-диалога.
        if (v0.isEmpty) {
          // Открыть диалог
          val v2 = MLogOutDia.empty
          updated( Some(v2) )
        } else {
          // Диалог уже открыт, игнорим дублирующийся экшен.
          noChange
        }

      } else if (m.pot.isReady) {
        // Всё ок, разлогиниваемся, стирая токен из хранилища.
        // TODO Эффект удаления токена только для cordova, в браузере - не нужно.
        var fxAcc: Effect = Effect.action {
          MKvStorage.delete( IdentConst.CookieToken.KV_STORAGE_TOKEN_KEY )
          DoNothing
        }

        // Надо совершить какие-то действия (перезагрузить страницу, например):
        for (fx <- diConfig.onLogOut())
          fxAcc = fxAcc >> fx

        updated( None, fxAcc )

      } else if (m.pot.isFailed) {
        // Тут ошибка запроса к серверу.
        val ex = m.pot.exceptionOption.get
        logger.warn( ErrorMsgs.SRV_REQUEST_FAILED, ex, m )
        v0.fold(noChange) { v00 =>
          val v2 = MLogOutDia.req.modify(_.fail(ex))(v00)
          updated( Some(v2) )
        }

      } else {
        noChange
      }


    // Нажатие кнопок в logout-диалоге.
    case m: LogoutConfirm =>
      (for {
        v0 <- value
      } yield {
        if (m.isLogout) {
          // Запустить logout-запрос на сервер:
          val fx = Effect {
            identApi
              .logout()
              .transform { tryResp =>
                val action = LogoutStep( Pot.empty.withTry(tryResp) )
                Success( action )
              }
          }
          val v2 = MLogOutDia.req.modify(_.pending())(v0)
          updated( Some(v2), fx )

        } else {
          // Отмена logout.
          updated( None )
        }
      })
      .getOrElse( noChange )

  }

}
