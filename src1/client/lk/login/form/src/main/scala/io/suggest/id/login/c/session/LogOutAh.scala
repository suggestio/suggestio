package io.suggest.id.login.c.session

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.c.IIdentApi
import io.suggest.id.login.m.{LoginFormDiConfig, LogoutConfirm, LogoutStep}
import io.suggest.id.login.m.session.MLogOutDia
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
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
{ ah =>

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
        // Всё ок: разлогиненная сессия получена с сервера, и будет записана отдельным вызовом. Просто скрыть logout-диалог.
        // TODO Эффект удаления токена только для cordova, в браузере - не нужно.
        // Надо совершить какие-то действия (перезагрузить страницу, например), когда требуется при компиляции:
        val fxOpt = diConfig.onLogOut()
        ah.updatedMaybeEffect( None, fxOpt )

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
          val v2 = MLogOutDia.req.modify(_.pending())(v0)
          // Запустить logout-запрос на сервер:
          val fx = Effect {
            identApi
              .logout()
              .transform { tryResp =>
                val action = LogoutStep( v2.req.withTry(tryResp) )
                Success( action )
              }
          }
          updated( Some(v2), fx )

        } else {
          // Отмена logout.
          updated( None )
        }
      })
        .getOrElse( noChange )

  }

}
