package io.suggest.sc.m.boot

import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.01.19 11:20
  * Description: Модель представления данных по одной службе.
  */
object MBootServiceState {

  @inline implicit def univEq: UnivEq[MBootServiceState] = UnivEq.derive

}


/**
  * Класс-обёртка для сохранения данных boot-контроллера.
  *
  * @param started None - запуска ещё не было.
  *                Some(Failure()) - запуск провалился.
  *                Some(false) - start() был вызван, но эффект ещё не завершён.
  *                Some(true) - start() был вызван и эффект завершился нормально.
  * @param tg Адаптер для управления конкретным процессом запуска.
  */
case class MBootServiceState(
                              tg                : IBootService,
                              started           : Option[Try[Boolean]]      = None,
                            ) {

  /** Свёрстка для значения started. */
  private def _foldStarted(isStartCompleted: Boolean => Boolean): Boolean =
    started.exists( _.fold(_ => false, isStartCompleted) )

  /** Был ли запущен ли сервис (вызван start)? */
  def isStarted: Boolean =
    _foldStarted( _ => true )

  /** Завершился ли start() успешно? */
  def isStartCompleted: Boolean =
    _foldStarted( identity[Boolean] )

  def withStarted(started: Option[Try[Boolean]]) = copy(started = started)

}
