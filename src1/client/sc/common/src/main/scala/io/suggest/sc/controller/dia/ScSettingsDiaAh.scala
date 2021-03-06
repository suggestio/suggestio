package io.suggest.sc.controller.dia

import diode.data._
import diode.{ActionHandler, ActionResult, Effect, ModelRW, UpdateSilent}
import io.suggest.radio.beacon.{BtOnOff, IBeaconsListenerApi, MBeaconerOpts}
import io.suggest.common.empty.OptionUtil
import io.suggest.conf.ConfConst
import io.suggest.i18n.MLanguages
import io.suggest.kv.MKvStorage
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.os.notify.NotifyStartStop
import io.suggest.sc.model.{GeoLocOnOff, LangSwitch, ResetUrlRoute, SettingAction, SettingSet, SettingsDiaOpen, SettingsRestore, WithSettings}
import io.suggest.sc.model.dia.settings.MScSettingsDia
import io.suggest.sc.sc3.MScSettingsData
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import japgolly.scalajs.react.extra.router.SetRouteVia
import play.api.libs.json.JsNull

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.2020 18:59
  * Description: Контроллер диалога настроек выдачи.
  */
class ScSettingsDiaAh[M](
                          modelRW    : ModelRW[M, MScSettingsDia],
                        )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  private def _failDataWith( v0: MScSettingsDia, ex: Throwable ): ActionResult[M] = {
    val v2 = MScSettingsDia.data.modify {
      _ .ready( MScSettingsData.empty )
        .fail( ex )
    }(v0)
    updatedSilent( v2 )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запрос на использование настройки для сборки опционального эффекта.
    case m: SettingAction =>
      val v0 = value
      // TODO Что-то тут не работает чтением/сохранением значений. Надо отладить.

      val settingValue = v0.data
        .toOption
        .flatMap(_.data.value.get( m.key ))
        .getOrElse( JsNull )

      m .fx( settingValue )
        .fold( noChange )( effectOnly )


    // Произвольное пакетное действие с настройками (чтение/запись).
    case m: WithSettings =>
      val v0 = value
      val data0 = v0.data getOrElse MScSettingsData.empty

      val actRes = m.action( data0 )

      var fxAcc = actRes.effectOpt.toList

      val v2Opt = for {
        settings2 <- actRes.newModelOpt
        if settings2 !===* data0
      } yield {
        if (!actRes.isInstanceOf[UpdateSilent])
          fxAcc ::= _saveSettingFx( settings2 )

        MScSettingsDia.data.modify(_.ready(settings2))(v0)
      }

      ah.optionalResult( v2Opt, fxAcc.mergeEffects, silent = !v0.opened )


    // Управление видимостью диалога настроек.
    case m: SettingsDiaOpen =>
      val v0 = value

      if (v0.opened ==* m.opened) {
        noChange

      } else {
        val v2 = (MScSettingsDia.opened replace m.opened)(v0)
        // Заменяем в истории, т.к. иначе откроется панель меню.
        val fx = Effect.action {
          ResetUrlRoute(
            via = SetRouteVia.HistoryReplace,
          )
        }
        updated(v2, fx)
      }


    // Редактирование настроек.
    case m: SettingSet =>
      val v0 = value

      // lazy, т.к. аргумент может быть не-boolean.
      lazy val isEnabled2 = m.value.as[Boolean]

      // Организуем эффект независимо от того, изменилась ли настройка - на случай каких-то ошибок,
      // чтобы они могли быть отработаны на нижнем уровне.
      // TODO These if-else spagetti below must be split and moved somewhere outside ScSettings controller, because is too specific for concrete cases/implementations.
      import ConfConst.{ScSettings => K}
      val okFxOpt: Option[Effect] = if (!m.runSideEffect) {
        // Force ignore setting change at current runtime, only save setting value into storage.
        None

      } else if (m.key ==* K.BLUETOOTH_BEACONS_ENABLED) {
        val fx = Effect.action {
          BtOnOff(
            isEnabled = OptionUtil.SomeBool( isEnabled2 ),
            opts = MBeaconerOpts(
              askEnableBt = true,
              oneShot     = false,
              scanMode    = IBeaconsListenerApi.ScanMode.BALANCED,
            ),
          )
        }
        Some(fx)

      } else if (m.key ==* K.LOCATION_ENABLED) {
        val fx = Effect.action {
          GeoLocOnOff(
            enabled = isEnabled2,
            isHard  = true,
          )
        }
        Some(fx)

      } else if (m.key ==* K.NOTIFICATIONS_ENABLED) {
        val fx = NotifyStartStop( isStart = isEnabled2 ).toEffectPure
        Some(fx)

      } else if (m.key ==* K.LANGUAGE) {
        val fx = Effect.action {
          LangSwitch(
            langOpt = m.value
              .asOpt[String]
              .flatMap( MLanguages.withValueOpt ),
          )
        }
        Some(fx)

      } else {
        None
      }

      var fxAcc = List.empty[Effect]
      for (okFx <- okFxOpt)
        fxAcc ::= okFx

      // If expected side-effect is missing, set error into state:
      val isSideEffectOk =
        !m.runSideEffect ||
        okFxOpt.nonEmpty ||
        (ConfConst.ScSettings.NO_SIDE_FX_ON_CHANGE contains m.key)

      if (!isSideEffectOk) {
        val msgCode = ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT
        logger.error( msgCode, msg = m )
        fxAcc ::= Effect.action {
          SettingsRestore( Pot.empty.fail( new IllegalArgumentException(msgCode) ) )
        }
      }

      val v2Opt = OptionUtil.maybeOpt( isSideEffectOk ) {
        val scSettings0 = v0.data getOrElse MScSettingsData.empty
        Option.when(
          !scSettings0.data
            .value
            .get( m.key )
            .contains( m.value )
        ) {
          val scSettings2 = MScSettingsData.data
            .modify(_ + (m.key -> m.value))( scSettings0 )

          val v2 = MScSettingsDia.data
            .modify( _.ready(scSettings2) )(v0)

          // If save=true and sideEffect is ok, and setting is changed, then save settings into storage.
          if (m.save)
            fxAcc ::= _saveSettingFx(scSettings2)

          v2
        }
      }

      ah.optionalResult( v2Opt, fxAcc.mergeEffects, silent = true )


    // Процесс восстановление ранее сохраненённых настроек.
    case m: SettingsRestore =>
      val v0 = value

      m.data match {

        // Запуск инициализации с нуля.
        case Empty =>
          // Эффект восстановления настроек.
          val fx = Effect.action {
            SettingsRestore(
              data = m.data withTry Try(
                MKvStorage
                  .get[MScSettingsData]( ConfConst.SC_SETTINGS_KEY )
                  .map(_.value)
              ),
            )
          }

          val v2 = MScSettingsDia.data.modify( _.pending() )(v0)
          updatedSilent( v2, fx )

        // Прочитан результат.
        case Ready(v) =>
          // Убрать pending:
          val v2 = MScSettingsDia.data.modify( _.ready(MScSettingsData.empty) )(v0)

          (for {
            savedData <- v
            fx <- savedData
              .data
              .value
              .iterator
              .map { case (k, v) =>
                SettingSet( k, v, save = false ).toEffectPure
              }
              .mergeEffects
          } yield {
            updatedSilent( v2, fx )
          })
            .getOrElse {
              updatedSilent(v2)
            }

        // Ошибочка вышла...
        case fb: FailedBase =>
          logger.error( ErrorMsgs.KV_STORAGE_ACTION_FAILED, fb.exception, m )
          _failDataWith(v0, fb.exception )

        case other =>
          logger.error( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT, msg = m )
          _failDataWith(v0, new UnsupportedOperationException( other.toString ) )

      }

  }


  private def _saveSettingFx(scSettings2: MScSettingsData): Effect = {
    Effect.action {
      val kvStor = MKvStorage( ConfConst.SC_SETTINGS_KEY, scSettings2 )
      MKvStorage.save( kvStor )
      DoNothing
    }
  }

}
