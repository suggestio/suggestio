package io.suggest.id.login.c

import diode._
import io.suggest.id.MEpwLoginReq
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.{MEpwLoginS, MEpwTextFieldS}
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:13
  * Description: EmailPw-контроллер.
  */
object EpwAh {

  /** Проверка и обновление значения btnActive. */
  private def epwLoginReqBtnActive(isActiveNow: Boolean, name: String, password: String, isPendingNow: Boolean,
                                   changesAcc: MEpwLoginS => MEpwLoginS): MEpwLoginS => MEpwLoginS = {
    val isActiveNext =
      !isPendingNow &&
      (name.length > 7) &&
      (password.length > 5)

    if (isActiveNow !=* isActiveNext) {
      changesAcc andThen MEpwLoginS.loginBtnEnabled.set( isActiveNext )
    } else
      changesAcc
  }

}
class EpwAh[M](
                loginApi        : ILoginApi,
                modelRW         : ModelRW[M, MEpwLoginS],
                returnUrlRO    : ModelRO[Option[String]],
              )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Ввод имени пользователя.
    case m: EpwSetName =>
      val v0 = value
      if (v0.name.value ==* m.name) {
        noChange
      } else {
        val v2 = EpwAh.epwLoginReqBtnActive(
          isActiveNow   = v0.loginBtnEnabled,
          name          = m.name,
          password      = v0.password.value,
          isPendingNow  = v0.loginReq.isPending,
          changesAcc    = MEpwLoginS.name
            .composeLens(MEpwTextFieldS.value)
            .set(m.name),
        )(v0)
        updated(v2)
      }


    // Ввод пароля.
    case m: EpwSetPassword =>
      val v0 = value
      if (v0.password.value ==* m.password) {
        noChange
      } else {
        val v2 = EpwAh.epwLoginReqBtnActive(
          isActiveNow   = v0.loginBtnEnabled,
          name          = v0.name.value,
          password      = m.password,
          isPendingNow  = v0.loginReq.isPending,
          changesAcc    = MEpwLoginS.password
            .composeLens(MEpwTextFieldS.value)
            .set(m.password),
        )(v0)
        updated(v2)
      }


    // Управление галочкой "Чужой компьютер"
    case m: EpwSetForeignPc =>
      val v0 = value
      if (v0.isForeignPc ==* m.isForeign) {
        noChange
      } else {
        val v2 = MEpwLoginS.isForeignPc.set( m.isForeign )( v0 )
        updated( v2 )
      }


    // Экшен запуска запроса на сервер с данными логина.
    case m @ EpwDoLogin =>
      val v0 = value
      if (v0.loginReq.isPending) {
        LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.loginReq) )
        noChange

      } else {
        val tstamp = System.currentTimeMillis()
        val req2 = v0.loginReq.pending( tstamp )

        val reqFx = Effect {
          loginApi
            .epw2Submit(
              MEpwLoginReq(
                name          = v0.name.value,
                password      = v0.password.value,
                isForeignPc   = v0.isForeignPc
              ),
              r = returnUrlRO.value
            )
            .transform { tryRes =>
              Success( EpwLoginResp(tstamp, tryRes) )
            }
        }

        val v2 = (
          MEpwLoginS.loginReq.set( req2 ) andThen
          MEpwLoginS.loginBtnEnabled.set( false )
        )(v0)

        updated( v2, reqFx )
      }


    // Ответ по логину-паролю.
    case m: EpwLoginResp =>
      val v0 = value
      if (v0.loginReq isPendingWithStartTime m.timestampMs) {
        // Пришёл ожидаемый ответ. Разобрать:
        val loginReq2 = m.tryRes.fold(
          v0.loginReq.fail,
          v0.loginReq.ready
        )
        // Если пришла ссылка и ок, то надо эффект редиректа организовать.
        val fxOpt = for (rdrUrl <- loginReq2.toOption) yield Effect.action {
          DomQuick.goToLocation( rdrUrl )
          DoNothing
        }

        var changesAcc = MEpwLoginS.loginReq.set( loginReq2 )

        val loginBtnEnabled2 = !m.tryRes.isSuccess
        if (v0.loginBtnEnabled !=* loginBtnEnabled2)
          changesAcc = changesAcc andThen MEpwLoginS.loginBtnEnabled.set( loginBtnEnabled2 )

        val v2 = changesAcc(v0)
        ah.updatedMaybeEffect( v2, fxOpt )

      } else {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
