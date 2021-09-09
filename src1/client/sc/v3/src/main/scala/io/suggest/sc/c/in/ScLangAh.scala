package io.suggest.sc.c.in

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.conf.ConfConst
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.MPlatformS
import io.suggest.i18n.{MLanguage, MLanguages}
import io.suggest.lk.api.ILkLangApi
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{LangInit, LangSwitch, SettingEffect, SettingSet}
import io.suggest.sc.m.in.MScReactCtx
import io.suggest.sc.u.api.IScStuffApi
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import play.api.libs.json.{JsNull, JsString, JsValue}
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._

import scala.util.Success

class ScLangAh[M](
                   modelRW      : ModelRW[M, MScReactCtx],
                   scStuffApi   : => IScStuffApi,
                   lkLangApi    : => ILkLangApi,
                   isLoggedIn   : ModelRO[Boolean],
                   platformRO   : ModelRO[MPlatformS],
                 )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Switch language signal.
    case m: LangSwitch =>
      val v0 = value

      if (m.state.isFailed || m.state.isPending) {
        for (ex <- m.state.exceptionOption)
          logger.error( ErrorMsgs.EXPECTED_FILE_MISSING, ex, m )

        val v2 = (MScReactCtx.langSwitch set m.state)(v0)
        updated(v2)

      } else if (m.state.isReady) {
        // Reset pot state, but update react context.
        val v2 = v0.copy(
          context = v0.context.copy(
            messages  = m.state.get,
          ),
          langSwitch  = Pot.empty.unavailable(),
          language    = m.langOpt,
          systemLang  = m.systemLangOpt,
        )

        var fxAcc = List.empty[Effect]

        // TODO Reload current grid/index using new language? So, 404-nodes (and ads) will be re-localiezed to current language.

        if (CordovaConstants.isCordovaPlatform()) {
          // Cordova: Save settings locally:
          fxAcc ::= Effect.action {
            SettingSet(
              key = ConfConst.ScSettings.LANGUAGE,
              value = m.langOpt.fold[JsValue]( JsNull ) { lang =>
                JsString( lang.value )
              },
              save = true,
            )
          }
        }

        // If logged in, also save on server:
        for (lang <- v2.languageOrSystem if isLoggedIn.value) {
          fxAcc ::= Effect.action {
            CsrfTokenEnsure(
              onComplete = Some( Effect {
                lkLangApi
                  .selectLangSubmit( lang )
                  .map( _ => DoNothing )
              })
            )
          }
        }

        ah.updatedMaybeEffect(v2, fxAcc.mergeEffects)

      } else if (
        (m.langOpt ==* v0.language) &&
        (v0.langSwitch !=* Pot.empty)
      ) {
        logger.log( ErrorMsgs.SUPPRESSED_INSUFFICIENT, msg = (m, v0) )
        noChange

      } else if (m.state.isEmpty) {
        // Fetch JSON data from server via ScSite API.
        // Use cordova file or use HTTP request? This will be resoluted by API.
        val pendingPot = v0.langSwitch.pending()
        val fx = Effect {
          val systemLangOpt = OptionUtil.maybeOpt( m.langOpt.isEmpty ) {
            WindowVm()
              .navigator
              .flatMap( _.language )
              .map( MLanguages.byCode )
          }
          // Try to detect language from env, if None lang sent.
          val langOpt2 = m.langOpt orElse systemLangOpt

          val plat = platformRO.value
          val cdvOsFamily = plat
            .osFamily
            .filter(_ => plat.isCordova)

          // Get messages from remote server (or from local filesystem - in cordova).
          scStuffApi
            .scMessagesJson( langOpt2, cdvOsFamily )
            .transform { tryRes =>
              Success( m.copy(
                state = m.state withTry tryRes,
                systemLangOpt = systemLangOpt,
              ))
            }
        }
        val v2 = (MScReactCtx.langSwitch set pendingPot)(v0)
        updated(v2, fx)

      } else {
        logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      }


    // Initialize language data.
    case LangInit =>
      val fx = Effect.action {
        SettingEffect(
          key = ConfConst.ScSettings.LANGUAGE,
          {jsValue =>
            val langOpt = jsValue
              .validateOpt[MLanguage]
              .getOrElse( None )
            Some( LangSwitch(langOpt).toEffectPure )
          }
        )
      }
      effectOnly( fx )

  }

}
