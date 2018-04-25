package io.suggest.scalaz

import io.suggest.common.empty.{EmptyUtil, IIsEmpty}
import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants

import scala.collection.{AbstractIterable, AbstractIterator, AbstractSeq}
import scalaz.{Foldable, IList, Monoid, NonEmptyList, Validation, ValidationNel}
import scalaz.syntax.foldable._

import scala.language.higherKinds
import scala.util.parsing.combinator.Parsers

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.17 21:47
  * Description: Утиль для упрощения работы с scalaz на ранних этапах.
  */
object ScalazUtil {


  /** generic validation function to accept anything that can be folded over along with
    * a function for transforming the data inside the containers
    *
    * @param in Container (data collection).
    * @param out Transform function.
    * @tparam F Collection type.
    * @tparam A Data element type.
    * @tparam B Outer result
    * @see Пародия на код, взятый отсюда [[https://www.47deg.com/blog/fp-for-the-average-joe-part-1-scalaz-validation/]]
    * @return Результат валидации.
    */
  // TODO Сделать B[_] вместо B?
  def validateAll[F[_] : Foldable, A, B: Monoid, E]
                 (in: F[A])
                 (out: A => ValidationNel[E, B]): ValidationNel[E, B] = {
    in.foldMap { a =>
      out(a)
    }
  }

  def liftNelOptMust[E, T](opt: Option[T], mustBeSome: Boolean, errMsg: => E)(f: T => ValidationNel[E, T]): ValidationNel[E, Option[T]] = {
    if (mustBeSome && opt.isDefined)
      liftNelSome(opt, errMsg)(f)
    else
      liftNelNone(opt, errMsg)
  }

  /** Приведение функции валидации к опциональной ипостаси.
    * None на входе всегда даёт success(None) на выходе.
    */
  def liftNelOpt[E, T](opt: Option[T])(f: T => ValidationNel[E, T]): ValidationNel[E, Option[T]] = {
    _liftNelOpt(opt, Validation.success(opt))(f)
  }
  /** Аналог liftNelOpt, но None приводит к ошибке. */
  def liftNelSome[E, T](opt: Option[T], errIfNone: => E)(f: T => ValidationNel[E, T]): ValidationNel[E, Option[T]] = {
    _liftNelOpt(opt, Validation.failureNel( errIfNone ))(f)
  }

  private def _liftNelOpt[E, T](opt: Option[T], empty: => ValidationNel[E, Option[T]])
                               (f: T => ValidationNel[E, T]): ValidationNel[E, Option[T]] = {
    // TODO Скорее всего, в scalaz есть более элегантное разруливание этого момента.
    opt.fold(empty) { v =>
      f(v)
        .map(EmptyUtil.someF)
    }
  }

  /** Валидация чего-то опционального, которое должно быть None. */
  def liftNelNone[E, T](opt: Option[T], nonEmptyError: => E): ValidationNel[E, Option[T]] = {
    opt.fold [ValidationNel[E, Option[T]]] ( Validation.success(opt) ) { _ =>
      Validation.failureNel(nonEmptyError)
    }
  }

  /** Неявно-пустая модель должна быть пустой.
    *
    * @param v Инстанс неявно-пустой модели.
    * @param nonEmptyError Ошибка, если модель непуста.
    * @return Результат валидации.
    */
  def liftNelEmpty[E, T <: IIsEmpty](v: T, nonEmptyError: => E): ValidationNel[E, T] = {
    if (v.isEmpty) {
      Validation.success( v )
    } else {
      Validation.failureNel(nonEmptyError)
    }
  }

  /** Аналог  */
  def someValidationOrFail[E, T](e: => E)(validationOpt: Option[ValidationNel[E, T]]): ValidationNel[E, T] = {
    validationOpt.getOrElse( Validation.failureNel(e) )
  }
  def validationOptOrNone[E, T](validationOpt: Option[ValidationNel[E, Option[T]]]): ValidationNel[E, Option[T]] = {
    validationOpt.getOrElse {
      Validation.success(None)
    }
  }
  def optValidationOrNone[E, T](validationOpt: Option[ValidationNel[E, T]]): ValidationNel[E, Option[T]] = {
    validationOpt
      .fold[ValidationNel[E, Option[T]]] (Validation.success(None)) ( _.map(Some.apply) )
  }


  def liftParseResult[E, T](pr: Parsers#ParseResult[T])(errorF: Parsers#NoSuccess => E): ValidationNel[E, T] = {
    pr match {
      case success: Parsers#Success[T] =>
        Validation.success( success.result )
      case noSuccess: Parsers#NoSuccess =>
        Validation.failureNel( errorF(noSuccess) )
    }
  }


  /** Валидация опционального текста по длине.
    * Если текст пустой, то будет None.
    * Если нет, то будет выверена макс.длина.
    *
    * @param textOpt Опциональный исходный текст.
    * @param maxLen Макс.длина строки текста.
    * @param errMsgF Сообщение об ошибке.
    * @return Результат валидации с ошибками или прочищенным текстом.
    */
  def validateTextOpt(textOpt: Option[String], maxLen: Int, errMsgF: => String): ValidationNel[String, Option[String]] = {
    ScalazUtil.liftNelOpt(
      textOpt
        .map(_.trim)
        .filter(_.length > 0)
    ) { text =>
      Validation.liftNel(text)(_.length > maxLen, errMsgF + HtmlConstants.`.` + ErrorConstants.Words.TOO_MANY)
    }
  }


  object Implicits {

    /** Доп.утиль для валидации. */
    implicit class RichValidationOpt[E, T]( val vldOpt: Option[ValidationNel[E, Option[T]]] ) extends AnyVal {

      /** Быстрый костылёк для валидации. */
      def getOrElseNone: ValidationNel[E, Option[T]] = {
        vldOpt.getOrElse {
          Validation.success(None)
        }
      }

    }


    /** API поддержки приведения IList'ов к нормальным человеческим коллекциям. */
    implicit class RichIList[T](ilist: IList[T]) { that =>

      /** Итератор для IList. */
      def iterator: Iterator[T] = {
        new AbstractIterator[T] {

          var xsOpt = Option(ilist)

          override def hasNext: Boolean = {
            xsOpt.exists(_.nonEmpty)
          }

          override def next(): T = {
            val xs = xsOpt.get
            val hd = xs.headOption.get
            xsOpt = xs.tailOption
            hd
          }

        }
      }

      /** O(1) iterable. */
      def asIterable: Iterable[T] = {
        new AbstractIterable[T] {
          override def iterator: Iterator[T] = that.iterator
        }
      }

      /** O(1) Seq. */
      def asSeq: Seq[T] = {
        new AbstractSeq[T] {
          override def apply(idx: Int): T = {
            val elOpt = iterator.zipWithIndex.find(_._2 == idx)
            elOpt.fold {
              throw new IndexOutOfBoundsException(s"No such index $idx for IList of size $length.")
            }(_._1)
          }
          override def length: Int = ilist.length
          override def iterator: Iterator[T] = that.iterator
        }
      }

    }


    /** Поддержка std api для NonEmptyList. */
    implicit class RichNel[T](nel: NonEmptyList[T]) extends RichIList(nel.list)

  }

}
