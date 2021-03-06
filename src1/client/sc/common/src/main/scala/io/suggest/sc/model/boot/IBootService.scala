package io.suggest.sc.model.boot

import diode.Effect
import japgolly.univeq.UnivEq

import scala.util.{Failure, Success, Try}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.01.19 22:21
  * Description: Описание одной цели инициализации.
  */
object IBootService {

  @inline implicit def univEq: UnivEq[IBootService] = UnivEq.force

  implicit class BootServiceExtOps( val bs: IBootService ) extends AnyVal {

    def finished(tryRes: Try[None.type]): BootStartCompleted =
      BootStartCompleted(bs.serviceId, tryRes)

    def finished(ex: Throwable): BootStartCompleted =
      finished( Failure(ex) )

    def finished(): BootStartCompleted =
      finished( Success(None) )

  }

}


/** Интерфейс одного boot-сервиса. */
trait IBootService {

  /** Уникальный ключ для данной цели инициализации. */
  def serviceId: MBootServiceId

  /** Сборка эффекта запуска цели на инициализацию.
    *
    * @return effect, который в итоге возвращает Future, которое обозначает успешное или провальное завершение запуска.
    */
  def startFx: Effect

  /** Список ключей целей инициализации, от которых зависит данная цель. */
  def depends: List[MBootServiceId] = Nil

}
