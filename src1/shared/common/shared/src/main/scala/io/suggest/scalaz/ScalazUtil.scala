package io.suggest.scalaz

import io.suggest.common.empty.{EmptyUtil, IIsEmpty}
import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants

import scala.collection.{AbstractIterable, AbstractIterator}
import scala.collection.immutable.AbstractSeq
import scalaz.{EphemeralStream, Foldable, ICons, IList, Monoid, NonEmptyList, Validation, ValidationNel}
import scalaz.syntax.foldable._
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.syntax.validation._

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
    * @see Sourced from [[https://www.47deg.com/blog/fp-for-the-average-joe-part-1-scalaz-validation/]]
    * @return Collection elements validation result.
    */
  // TODO Map[_,_] перестал работать в scala-2.13 с scalaz-7.2.29.
  def validateAll[F[_] : Foldable, A, B: Monoid, E]
                 (in: F[A])
                 (out: A => ValidationNel[E, B]): ValidationNel[E, B] = {
    in.foldMap(out)
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
    _liftNelOpt( opt, opt.successNel[E] )(f)
  }
  /** Аналог liftNelOpt, но None приводит к ошибке. */
  def liftNelSome[E, T1, T2](opt: Option[T1], errIfNone: => E)(f: T1 => ValidationNel[E, T2]): ValidationNel[E, Option[T2]] = {
    _liftNelOpt( opt, errIfNone.failureNel[Option[T2]] )(f)
  }

  private def _liftNelOpt[E, T1, T2](opt: Option[T1], empty: => ValidationNel[E, Option[T2]])
                                    (f: T1 => ValidationNel[E, T2]): ValidationNel[E, Option[T2]] = {
    // TODO Скорее всего, в scalaz есть более элегантное разруливание этого момента.
    opt.fold(empty) { v =>
      f(v)
        .map(EmptyUtil.someF)
    }
  }

  /** Валидация чего-то опционального, которое должно быть None. */
  def liftNelNone[E, T](opt: Option[T], nonEmptyError: => E): ValidationNel[E, Option[T]] = {
    if (opt.isEmpty) opt.successNel
    else nonEmptyError.failureNel
  }

  /** Неявно-пустая модель должна быть пустой.
    *
    * @param v Инстанс неявно-пустой модели.
    * @param nonEmptyError Ошибка, если модель непуста.
    * @return Результат валидации.
    */
  def liftNelEmpty[E, T <: IIsEmpty](v: T, nonEmptyError: => E): ValidationNel[E, T] = {
    if (v.isEmpty) v.successNel
    else nonEmptyError.failureNel
  }

  /** Аналог  */
  def someValidationOrFail[E, T](e: => E)(validationOpt: Option[ValidationNel[E, T]]): ValidationNel[E, T] = {
    validationOpt.getOrElse( e.failureNel )
  }
  def validationOptOrNone[E, T](validationOpt: Option[ValidationNel[E, Option[T]]]): ValidationNel[E, Option[T]] = {
    validationOpt.getOrElse {
      Option.empty[T].successNel
    }
  }
  def optValidationOrNone[E, T](validationOpt: Option[ValidationNel[E, T]]): ValidationNel[E, Option[T]] = {
    validationOpt
      .fold( Option.empty[T].successNel[E] ) ( _.map(Some.apply) )
  }


  def liftParseResult[E, T](pr: Parsers#ParseResult[T])(errorF: Parsers#NoSuccess => E): ValidationNel[E, T] = {
    pr match {
      case success: Parsers#Success[T] =>
        success.result.successNel
      case noSuccess: Parsers#NoSuccess =>
        errorF(noSuccess).failureNel
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
    liftNelOpt(
      textOpt
        .map(_.trim)
        .filter(_.length > 0)
    ) { text =>
      Validation.liftNel(text)(_.length > maxLen, errMsgF + HtmlConstants.`.` + ErrorConstants.Words.TOO_MANY)
    }
  }


  /** Помимо индексированного NonEmptyList[A], если длина == 1, вернуть [де]сериализовать единственное значение. */
  def nelOrSingleValueJson[A: Format]: Format[NonEmptyList[A]] = {
    val nelFmt = Implicits.nelJson[A]

    val writes2 = nelFmt.transform {
      case JsArray(arr) if arr.lengthIs == 1 =>
        arr.head
      case other => other
    }
    val reads2 = nelFmt.orElse {
      implicitly[Reads[A]]
        .map( NonEmptyList(_) )
    }

    Format(reads2, writes2)
  }


  object Implicits {

    @inline implicit def ephemeralStreamUe[T: UnivEq]: UnivEq[EphemeralStream[T]] = UnivEq.force

    /** Поддержка play-json для Scalaz IList. */
    implicit def ilistJson[A: Format]: Format[IList[A]] = {
      implicitly[Format[List[A]]]
        .inmap [IList[A]] ( IList.fromList, _.toList )
    }


    /** Поддержка play-json для NonEmptyList. */
    implicit def nelJson[A: Format]: Format[NonEmptyList[A]] = {
      val ilistFmt = ilistJson[A]

      val readsNel = ilistFmt.flatMap [NonEmptyList[A]] {
        case ICons(h, t) =>
          Reads.pure( NonEmptyList.nel(h, t) )
        case _ =>
          Reads.failed( ErrorConstants.Words.MISSING )
      }

      val writesNel = ilistFmt.contramap[NonEmptyList[A]]( _.list )
      Format( readsNel, writesNel )
    }


    /** Доп.утиль для валидации. */
    implicit final class RichValidationOpt[E, T]( private val vldOpt: Option[ValidationNel[E, Option[T]]] ) extends AnyVal {

      /** Быстрый костылёк для валидации. */
      def getOrElseNone: ValidationNel[E, Option[T]] = {
        vldOpt.getOrElse {
          Option.empty[T].successNel
        }
      }

    }


    /** API поддержки приведения IList'ов к нормальным человеческим коллекциям. */
    implicit class RichIList[T]( private val ilist: IList[T] ) { that =>

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
            xsOpt = xs.tailMaybe.toOption
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
            val elOpt = iterator.zipWithIndex.find(_._2 ==* idx)
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
    implicit final class RichNel[T](nel: NonEmptyList[T]) extends RichIList(nel.list)


    implicit final class EphStreamExt[A]( private val eph: EphemeralStream[A] ) extends AnyVal {
      def toIterable = EphemeralStream.toIterable( eph )
      def iterator = toIterable.iterator
    }

    /** Доп.API к статическому EphemeralStream. */
    implicit final class EphStreamStaticExt( private val ephSt: EphemeralStream.type ) extends AnyVal {

      /** Представление LazyList в EphemeralStream.
        *
        * scalaz-7.3.2: В scalaz master уже есть этот статический метод, но в релизе нет.
        * Удалить этот метод, когда будет нормальная поддержка в обычном EphemeralStream.
        *
        * @see [[https://github.com/scalaz/scalaz/blob/master/core/src/main/scala/scalaz/EphemeralStream.scala#L341]]
        */
      def fromLazyList[A](s: LazyList[A]): EphemeralStream[A] = {
        s match {
          case h #:: t      => ephSt.cons(h, fromLazyList(t))
          case _            => ephSt.emptyEphemeralStream
        }
      }

      def fromList[A](s: List[A]): EphemeralStream[A] = {
        s match {
          case h :: t       => ephSt.cons(h, fromList(t))
          case Nil          => ephSt.emptyEphemeralStream
        }
      }

      /** Не быстрая и неэффективная разборка абстрактного множества в ephemeral-список.
        * Это добро следует использовать только для небольших множеств. */
      def fromSet[A](s: Set[A]): EphemeralStream[A] = {
        if (s.isEmpty)
          ephSt.emptyEphemeralStream
        else
          ephSt.cons( s.head, fromSet(s.tail) )
      }

      def fromOption[A](s: Option[A]): EphemeralStream[A] = {
        s.fold [EphemeralStream[A]] ( ephSt[A] )( ephSt.cons(_, ephSt[A]) )
      }

    }

    implicit final class SciLazyListExt[T]( private val ll: LazyList[T] ) extends AnyVal {
      def toEphemeralStream: EphemeralStream[T] =
        EphemeralStream.fromLazyList( ll )
    }

    implicit final class SciListExt[T]( private val l: List[T]) extends AnyVal {
      def toEphemeralStream: EphemeralStream[T] =
        EphemeralStream.fromList(l)
    }

    implicit final class SciSetExt[T]( private val s: Set[T]) extends AnyVal {
      def toEphemeralStream: EphemeralStream[T] =
        EphemeralStream.fromSet( s )
    }

    implicit final class OptionExt[T]( private val o: Option[T] ) extends AnyVal {
      def toEphemeralStream: EphemeralStream[T] =
        EphemeralStream.fromOption( o )
    }

  }

}
