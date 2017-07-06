package io.suggest.scalaz

import scala.collection.{AbstractIterable, AbstractIterator, AbstractSeq}
import scalaz.{Foldable, IList, Monoid, NonEmptyList, ValidationNel}
import scalaz.syntax.foldable._
import scala.language.higherKinds

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
  def validateAll[F[_] : Foldable, A, B: Monoid]
                 (in: F[A])
                 (out: A => ValidationNel[String, B]): ValidationNel[String, B] = {
    in.foldMap { a =>
      out(a)
    }
  }


  object Implicits {

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


  /** Всякие implicit'ы, с которыми надо бы аккуратнее. */
  object HellImplicits {

    implicit def StringMonoid: Monoid[String] = new Monoid[String] {
      override def zero: String = ""
      override def append(f1: String, f2: => String): String = f1 + f2
    }

  }

}
