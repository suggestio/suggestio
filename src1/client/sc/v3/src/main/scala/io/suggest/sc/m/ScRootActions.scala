package io.suggest.sc.m

import io.suggest.geo.{GeoLocType, MGeoLoc, PositionException}
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.{MSc3Resp, MScQs, MScRespActionType}
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import io.suggest.spa.DAction

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
/** Интерфейс для Index-resp reason. */
trait IScIndexRespReason extends IScApiRespReason {
  def focusedAdId: Option[String] = None
}


/** Интерфейс корневых экшенов. */
sealed trait IScRootAction extends DAction

/** Запустить инициализацию js-роутера. */
case object JsRouterInit extends IScRootAction

/** Сигнал основной цепочке о состоянии основного js-роутера. */
case class JsRouterStatus( payload: Try[ScJsRoutes.type] ) extends IScRootAction


/** События экрана. */
case object ScreenReset extends IScRootAction

/** Сработал таймер непосредственного запуска действий при ресайзе. */
case object ScreenRszTimer extends IScRootAction


/** Управление подсистемой геолокации в режиме вкл/выкл.
  *
  * @param enabled Включена?
  * @param onlyTypes изменение касается только указанных типов. [] эквивалентен GeoLocTypes.all
  */
case class GeoLocOnOff(enabled: Boolean, onlyTypes: Traversable[GeoLocType] = Nil) extends IScRootAction


/** trait только для [[GlLocation]] и [[GlError]]. */
sealed trait IGeoLocSignal extends IScRootAction {
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

/** Сигнал о наступлении геолокации (или ошибке оной) для ожидающего геолокацию. */
case class GlPubSignal( orig: IGeoLocSignal ) extends IScRootAction


/** Сработал таймер подавления нежелательных координат. */
case class GlSuppressTimeout(generation: Long) extends IScRootAction


/** Из js-роутера пришла весточка, что нужно обновить состояние из данных в URL. */
case class RouteTo( mainScreen: MainScreen ) extends IScRootAction

/** Команда к обновлению ссылки в адресе согласно обновившемуся состоянию. */
case object ResetUrlRoute extends IScRootAction


/** Наступление таймаута получения гео-координат. */
case object GeoLocTimeOut extends ISc3Action


/** Экшен для запуска обработки унифицированного ответа выдачи, который бывает сложным и много-гранным.
  * @param reqTimeStamp Время запуска запроса к серверу.
  * @param tryResp Результат запроса к серверу.
  * @param apiReq Оригинальный реквест к api выдачи.
  * @param reason Оригинальный исходный экшен, с которого всё действо началось.
  */
case class HandleScApiResp(
                            reqTimeStamp   : Option[Long],
                            apiReq         : MScQs,
                            tryResp        : Try[MSc3Resp],
                            reason         : IScApiRespReason
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

}


/** Изменилась видимость выдачи, нужно приостановить или возобновить работу выдачи. */
case class PauseOrResume(isScVisible: Boolean) extends IScRootAction

/** Сигнал готовности платформы к полноценной работе. */
case object SetPlatformReady extends IScRootAction

