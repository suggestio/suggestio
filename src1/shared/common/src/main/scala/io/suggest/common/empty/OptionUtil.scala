package io.suggest.common.empty

import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

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
  @inline
  def maybe[T](isSome: Boolean)(someF: => T): Option[T] = {
    if (isSome)
      Some(someF)
    else
      None
  }

  /** Вернуть Some(true) или None. */
  def maybeTrue(isSome: Boolean) = maybe(isSome)(isSome)


  @inline
  def maybeOpt[T](isSome: Boolean)(optF: => Option[T]): Option[T] = {
    if (isSome)
      optF
    else
      None
  }

  def maybeFut[T](isSome: Boolean)(someF: => Future[Option[T]]): Future[Option[T]] = {
    if (isSome)
      someF
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


  /** Дополнительные операции для Option[Boolean]. */
  implicit class BoolOptOps(val boolOpt: Option[Boolean]) extends AnyVal {

    /** Оптимизированный аналог boolOpt.getOrElse(false).
      * Позволяет обойтись без функции, хотя начиная со scala 2.12 это не очень актуально.
      */
    def getOrElseFalse: Boolean = {
      boolOpt.contains(true)
    }

    def getOrElseTrue: Boolean = {
      boolOpt.getOrElse(true)
    }

  }


  /** Расширенные фунции для nullable-json-форматирования boolean. */
  implicit class BoolOptJsonFormatOps(val boolOptFormat: OFormat[Option[Boolean]]) extends AnyVal {
    import play.api.libs.functional.syntax._

    /** Приведение nullable-json-форматтера к boolean-форматтеру, для которого false==None */
    def formatBooleanOrFalse: OFormat[Boolean] = {
      boolOptFormat.inmap(_.getOrElseFalse, maybeTrue)
    }

  }


  def ofTypeF[X, T <: X: ClassTag]: PartialFunction[X, Option[T]] = {
    case t: T => Some(t)
    case _    => None
  }

  implicit class AnyOptOps[X](val option: Option[X]) extends AnyVal {

    def filterByType[T <: X: ClassTag]: Option[T] = {
      option.flatMap( ofTypeF[X, T] )
    }

  }

}
