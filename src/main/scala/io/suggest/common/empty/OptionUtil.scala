package io.suggest.common.empty

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.16 14:10
  * Description: Дополнительная утиль для scala option'ов.
  */
object OptionUtil {

  /**
    * Условно собрать Option[T]. Нужно для снижения кол-ва строк в сорцах.
    * @param isSome false => None
    *               true  => Some(f())
    * @return Опциональный результат выполнения функции f в зависимости от значения isSome.
    */
  def maybe[T](isSome: Boolean)(f: => T): Option[T] = {
    if (isSome)
      Some(f)
    else
      None
  }

  def maybeFut[T](isSome: Boolean)(f: => Future[Option[T]]): Future[Option[T]] = {
    if (isSome)
      f
    else
      Future.successful(None)
  }


  /** Аналог orElse для асинхронных опшинов. */
  def orElseFut[T](optFut: Future[Option[T]])(orElse: => Future[Option[T]])(implicit ec: ExecutionContext): Future[Option[T]] = {
    optFut.flatMap {
      case None => orElse
      case some => Future.successful(some)
    }
  }

}
