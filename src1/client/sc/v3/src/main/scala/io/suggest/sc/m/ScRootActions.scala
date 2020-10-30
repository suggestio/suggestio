package io.suggest.sc.m

import diode.data.Pot
import io.suggest.geo.{GeoLocType, MGeoLoc, PositionException}
import io.suggest.lk.nodes.form.m.MLkNodesMode
import io.suggest.routes.routes
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.m.dev.{GlLeafletLocateArgs, MOnLineInfo}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sc.m.inx.save.MIndexesRecent
import io.suggest.sc.sc3.{MSc3Resp, MScQs, MScRespActionType}
import io.suggest.sjs.dom2.GeoLocWatchId_t
import io.suggest.spa.{DAction, SioPages}
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:30
  * Description: Корневые экшены sc3.
  */

/** Маркер-интерфейс для экшенов sc3. */
trait ISc3Action extends DAction

/** Интерфейс для допустимых значений поля HandleScApiResp.tryResp. */
trait IScApiRespReason extends ISc3Action
/** Интерфейс-маркер для Index-resp reason. */
trait IScIndexRespReason extends IScApiRespReason


/** Интерфейс корневых экшенов. */
sealed trait IScRootAction extends DAction

/** Запустить инициализацию js-роутера. */
case object JsRouterInit extends IScRootAction

/** Сигнал основной цепочке о состоянии основного js-роутера. */
case class JsRouterStatus( payload: Try[routes.type] ) extends IScRootAction


/** События экрана. */
case object ScreenResetPrepare extends IScRootAction

/** Сработал таймер непосредственного запуска действий при ресайзе. */
case object ScreenResetNow extends IScRootAction


/** Интерфейс экшенов для GeoLocAh. */
sealed trait IGeoLocAction extends IScRootAction

/** Управление подсистемой геолокации в режиме вкл/выкл.
  *
  * @param enabled Включена?
  * @param isHard Жестко выключено силами юзера?
  * @param onlyTypes изменение касается только указанных типов. [] эквивалентен GeoLocTypes.all
  */
case class GeoLocOnOff(
                        enabled     : Boolean,
                        isHard      : Boolean,
                        onlyTypes   : Iterable[GeoLocType] = Nil,
                        // TODO Записывать это напрямую в состояние из circuit? Или эффектом отдельным? Или...?
                        scSwitch    : Option[MScSwitchCtx]    = None,
                      )
  extends IGeoLocAction

/** trait только для [[GlLocation]] и [[GlError]]. */
sealed trait IGeoLocSignal extends IGeoLocAction {
  def glType: GeoLocType
  def isSuccess: Boolean
  final def isError: Boolean = !isSuccess
  def locationOpt: Option[MGeoLoc] = None
  def errorOpt: Option[PositionException] = None
  def either: Either[PositionException, MGeoLoc]
}

/** Есть координаты. */
case class GlLocation(override val glType: GeoLocType, location: MGeoLoc) extends IGeoLocSignal {
  override def isSuccess = true
  override def locationOpt = Some(location)
  override def either = Right(location)
}
/** Ошибка получения координат. */
case class GlError(override val glType: GeoLocType, error: PositionException) extends IGeoLocSignal {
  override def isSuccess = false
  override def errorOpt = Some(error)
  override def either = Left(error)
}

/** Обновление gl watcher'ов в состоянии новыми идентификаторами. */
case class GlModWatchers( watchers: Map[GeoLocType, Pot[GeoLocWatchId_t]] ) extends IGeoLocAction

/** Сигнал о наступлении геолокации (или ошибке оной) для ожидающего геолокацию. */
case class GlPubSignal( origOpt: Option[IGeoLocSignal], scSwitch: Option[MScSwitchCtx] ) extends IGeoLocAction


/** Сработал таймер подавления нежелательных координат. */
case class GlSuppressTimeout(generation: Long) extends IGeoLocAction

/** Запросить восстановление работы геолокации с перемещением в новую точку. */
case object GlCheckAfterResume extends IGeoLocAction

/** Переброска в GeoLocAh запроса из Leaflet.Map().locate() и stopLocation(). */
case class GlLeafletApiLocate(locateOpts: Option[GlLeafletLocateArgs] ) extends IGeoLocAction
/** Таймаут запроса геолокации из leaflet. */
case object GlLeafletApiLocateTimeout extends IGeoLocAction

/** Из js-роутера пришла весточка, что нужно обновить состояние из данных в URL. */
case class RouteTo( mainScreen: SioPages.Sc3 ) extends IScRootAction

/** Команда к обновлению ссылки в адресе согласно обновившемуся состоянию.
  * @param mods Функция обновления роуты.
  *             Исходное значение роуты передаётся как функция, потому что оно лениво и не всегда нужно.
  * @param force Не проверять роуту на предмет дубликата, пробрасывать в sjsreact-router даже повторную роуту.
  */
case class ResetUrlRoute(
                          mods: Option[(() => SioPages.Sc3) => SioPages.Sc3] = None,
                          force: Boolean = false
                        )
  extends IScRootAction

/** Восстановление текущего состояния ранее-посещённых индексов. */
case class LoadIndexRecents(clean: Boolean) extends IScRootAction

/** Сохранить в базу инфу по индексу. */
case class SaveRecentIndex(inxRecent2: Option[MIndexesRecent] = None) extends IScRootAction

/** Выбор узла в списке недавних узлов. */
case class IndexRecentNodeClick( inxRecent: MSc3IndexResp ) extends IScRootAction


/** Экшен хардварной кнопки "Назад", которую надо отрабатывать по-особому. */
case object HwBack extends IScRootAction

/** Запуск таймера ожидания получения гео-координат. */
case class GeoLocTimerStart( switchCtx: MScSwitchCtx ) extends ISc3Action

/** Наступление таймаута получения гео-координат. */
case class GeoLocTimeOut( switchCtx: MScSwitchCtx ) extends ISc3Action


/** Экшен для запуска обработки унифицированного ответа выдачи, который бывает сложным и много-гранным.
  * @param reqTimeStamp Время запуска запроса к серверу.
  * @param tryResp Результат запроса к серверу.
  * @param qs Оригинальный реквест к api выдачи.
  * @param reason Оригинальный исходный экшен, с которого всё действо началось.
  * @param switchCtxOpt Опциональный контекст глобального переключения выдачи.
  */
case class HandleScApiResp(
                            reqTimeStamp   : Option[Long],
                            qs             : MScQs,
                            tryResp        : Try[MSc3Resp],
                            reason         : IScApiRespReason,
                            switchCtxOpt   : Option[MScSwitchCtx] = None,
                          )
  extends ISc3Action
{

  /** Проверить тип экшена ответа. */
  def isNextRespActionType(expected: MScRespActionType): Boolean = {
    tryResp.isSuccess &&
      tryResp.get.isNextActionType( expected )
  }

  /** Обёртка над copy() при обновлении экшена (перещёлкивание экшенов в tryResp).
    *
    * @param tryResp Обновлённые данные ответа сервера.
    * @param reqTimeStamp Обычно тут None, что обозначает отсутствие проверки timestamp.
    * @return Обновлённый инстанс [[HandleScApiResp]].
    */
  def withTryRespTs(tryResp: Try[MSc3Resp], reqTimeStamp: Option[Long] = None) =
    copy(tryResp = tryResp, reqTimeStamp = reqTimeStamp)

  def withSwitchCtxOpt(switchCtxOpt: Option[MScSwitchCtx]) =
    copy(switchCtxOpt = switchCtxOpt)

  override def toString: String = {
    StringUtil.toStringHelper(this, 512) { renderF =>
      val render0 = renderF("")
      reqTimeStamp foreach render0
      renderF("qs")( qs )
      renderF("res")( tryResp )
      renderF("rsn")( reason )
      switchCtxOpt foreach render0
    }
  }

}
object HandleScApiResp {
  @inline implicit def univEq: UnivEq[HandleScApiResp] = UnivEq.force
}


sealed trait IPlatformAction extends IScRootAction

/** Изменилась видимость выдачи, нужно приостановить или возобновить работу выдачи. */
case class PauseOrResume(isScVisible: Boolean) extends IPlatformAction

/** Сигнал готовности платформы к полноценной работе. */
case object SetPlatformReady extends IPlatformAction


case class UpdateUnsafeScreenOffsetBy(incDecBy: Int) extends IScRootAction



/** Маркер-интерфейс для экшенов для ErrorAh: */
sealed trait IScErrorAction extends IScRootAction

/** Команда повтора при ошибке. */
case object RetryError extends IScErrorAction

case object CheckRetryError extends IScErrorAction

/** Закрытие диалога об ошибке. */
case object CloseError extends IScErrorAction

/** Выставление состояния ошибки. */
case class SetErrorState(scErr: MScErrorDia) extends IScErrorAction


/** Управление отображением диалога настроек выдачи. */
case class SettingsDiaOpen(opened: Boolean) extends IScErrorAction



sealed trait IScDaemonAction extends IScRootAction

/** Уход приложения в фон, но без демон-процесса. */
case class ScDaemonDozed( isActive: Boolean ) extends IScDaemonAction

/** Активация или деактивация демон-процесса работы. */
case class ScDaemonWorkProcess( isActive: Boolean ) extends IScDaemonAction

/** Запущен/остановлен запасной таймер засыпания демона. */
case class ScDaemonFallSleepTimerSet( timerId: Option[Int] ) extends IScDaemonAction

/** Срабатывание таймера пробуждения демона. */
case class ScDaemonSleepAlarm(isActive: Boolean) extends IScDaemonAction


sealed trait IOnlineAction extends DAction

/** (Пере)инициализация online-состояния. */
case class OnlineInit(init: Boolean) extends IOnlineAction

/** Инициализация или ручная проверка connectivity. */
case object OnlineCheckConn extends IOnlineAction

/** Результат [[OnlineCheckConn]]. */
case class OnlineCheckConnRes( netInfo: MOnLineInfo ) extends IOnlineAction


sealed trait IScConfAction extends IScRootAction
/** Замена текущего значения debug-флага. */
case class SetDebug(isDebug: Boolean) extends IScConfAction


/** Экшены для sc-login. */
sealed trait IScLoginAction extends IScRootAction

case class ScLoginFormShowHide( visible: Boolean ) extends IScLoginAction
/** Изменение внешнего (spa-router) состояния модуля login-формы. */
case class ScLoginFormChange(loginPageOpt: Option[SioPages.Login] ) extends IScLoginAction


/** Экшены для sc-nodes. */
sealed trait IScNodesAction extends IScRootAction

/** Переключение состояния формы отображения. */
case class ScNodesShowHide( visible: Boolean ) extends IScNodesAction

/** Смена режима работы nodes-формы. */
case class ScNodesModeChanged( mode: MLkNodesMode ) extends IScNodesAction

/** Экшен управление подпиской sc-nodes на события BLE beaconer. */
case class ScNodesBcnrSubscribeStatus(unSubsCribeF: Pot[() => Unit] = Pot.empty ) extends IScNodesAction
