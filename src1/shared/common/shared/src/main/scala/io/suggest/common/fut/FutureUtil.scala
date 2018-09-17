package io.suggest.common.fut

import io.suggest.common.empty.EmptyUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 20:31
 * Description: Утиль для Future.
 */
object FutureUtil {

  /** Жесткие implicit'ы. Следует юзать аккуратно, т.к. могут быть проблемы. */
  object HellImplicits {

    implicit def ex2fut[T](ex: Throwable): Future[T] = {
      Future.failed(ex)
    }

    implicit def any2fut[T](x: T): Future[T] = {
      Future.successful(x)
    }

  }


  import HellImplicits._

  /**
   * Перехват синхронных ошибок для вызовов, которые должны возвращать их асинхронно.
   * @param f Функция, потенциально возвращающая Future[X] либо асинхронную ошибку.
   * @tparam X Тип возвращаемого значения.
   * @return Future[X] и никаких исключений.
   */
  def tryCatchFut[X](f: => Future[X]): Future[X] = {
    try {
      f
    } catch {
      case ex: Throwable =>
        ex
    }
  }


  /**
   * Часто бывает необходимость сверстки Option[T] в фьючерс с опциональным значением.
   * Тут код для укорачивания такой сверстки.
   * @param opt Исходное опциональное значение.
   * @param defined Функция, вызываемая для исходного значения T.
   * @tparam T Тип исходного значения.
   * @tparam R Тип результирующего значения.
   * @return Фьючерс с опциональным результатом.
   */
  def optFut2futOpt[T, R](opt: Option[T])(defined: T => Future[Option[R]]): Future[Option[R]] = {
    opt.fold [Future[Option[R]]] {
      Option.empty[R]
    } { v =>
      defined(v)
    }
  }


  def optFut2futOptPlain[T](opt: Option[Future[T]])(implicit ec: ExecutionContext): Future[Option[T]] = {
    opt.fold[Future[Option[T]]] {
      Option.empty[T]
    } { fut0 =>
      fut0.map( EmptyUtil.someF )
    }
  }


  /**
   * Иногда бывает нужно опциональное значение трансформировать в неопциональный фьючерс.
   * @param x Option[T]
   * @param ifEmpty Фьючерс, когда нет значения.
   * @tparam T Тип отрабатываемого значения.
   * @return Future[T].
   */
  def opt2future[T](x: Option[T])(ifEmpty: => Future[T]): Future[T] = {
    x.fold(ifEmpty)(Future.successful)
  }


  def opt2futureOpt[T](x: Option[T])(ifEmpty: => Future[Option[T]]): Future[Option[T]] = {
    x.fold(ifEmpty)(_ => Future.successful(x))
  }


  object Implicits {

    implicit class FutureExtOps[T](val fut: Future[T]) extends AnyVal {

      /** Заворачивание возможного NSEE в None, а результата в Some().
        *
        * @return Фьючерс с опциональным результатом.
        */
      def toOptFut(implicit ec: ExecutionContext): Future[Option[T]] = {
        fut
          .map[Option[T]] { EmptyUtil.someF }
          .recover { case _: NoSuchElementException =>
            None
          }
      }

    }

  }

}
