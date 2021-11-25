package io.suggest.sc.model.boot

import diode.Effect
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.01.19 11:20
  * Description: Модель представления данных по одной службе.
  */
object MBootServiceState {

  @inline implicit def univEq: UnivEq[MBootServiceState] = UnivEq.force

  def started = GenLens[MBootServiceState](_.started)
  def after = GenLens[MBootServiceState](_.after)

}


/**
  * Класс-обёртка для сохранения данных boot-контроллера.
  *
  * @param started None - запуска ещё не было.
  *                Some(Failure()) - запуск провалился.
  *                Some(false) - start() был вызван, но эффект ещё не завершён.
  *                Some(true) - start() был вызван и эффект завершился нормально.
  * @param tg Адаптер для управления конкретным процессом запуска.
  * @param after Запустить указаный эффект после успешного запуска сервиса.
  *              Появилось для возможности разруливания race-conditions между JsRouterInit и преждевременным _reRouteFx.
  */
case class MBootServiceState(
                              tg                : IBootService,
                              started           : Option[Try[Boolean]]      = None, // TODO Replace with Pot[Boolean] ?
                              after             : Option[Effect]            = None,
                            ) {

  /** Свёрстка для значения started. */
  private def _foldStarted(isStartCompleted: Boolean => Boolean, onError: Boolean): Boolean =
    started.exists( _.fold(_ => onError, isStartCompleted) )

  /** Был ли запущен ли сервис (вызван start)? */
  def wasStarted: Boolean =
    _foldStarted( _ => true, onError = true )

  /** Завершился ли start() успешно? */
  def isStartDoneSuccess: Boolean =
    _foldStarted( identity[Boolean], onError = false )

}
