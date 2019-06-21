package io.suggest.scalaz

import japgolly.univeq._
import monocle._
import monocle.function.{At, Index}
import scalaz.Applicative
import scalaz.std.map._
import scalaz.syntax.traverse._
import scalaz.syntax.applicative._

import scala.collection.immutable.ListMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.06.19 21:55
  * Description: Утиль для monocle.
  */
object MonocleUtil {

  object map {

    /** Траверс для работы в контексте определённых ключей ассоциативного массива.
      *
      * @see [[http://julien-truffaut.github.io/Monocle/optics/traversal.html code example from here]]
      *
      * @param predicate Функция-предикат, которая осуществляет фильтрацию ключа.
      * @tparam K Тип ключа.
      * @tparam V Тип значения.
      * @return Траверс по значениям для подходящих ключей.
      */
    def filterKey[K, V](predicate: K => Boolean): Traversal[Map[K, V], V] = {
      new Traversal[Map[K, V], V] {
        override def modifyF[F[_]: Applicative](f: V => F[V])(s: Map[K, V]): F[Map[K, V]] = {
          s.map { case (k, v) =>
            val v2 = if ( predicate(k) ) f(v)
                     else v.pure[F]
            k -> v2
          }.sequenceU
        }
      }
    }

    /** filterKey-траверс для одного ключа. */
    // TODO Opt Лучше юзать composeOptional( at(key) ) - должно быть быстрее.
    def get[K: UnivEq, V](key: K) = filterKey[K, V](key ==* )

  }


  object kvList {

    /** Работа в списке с первым попавшимся элементом с указанным ключом. */
    implicit def kvListIndex[K: UnivEq, V]: Index[List[(K,V)], K, V] = {
      Index { i =>
        Optional[List[(K, V)], V] { els =>
          els
            .find(_._1 ==* i)
            .map(_._2)
        } { v2 => s =>
          s.map { case kv0 @ (k, _) =>
            if (k ==* i) k -> v2 else kv0
          }
        }
      }
    }

  }


  object listMap {

    implicit def listMapAt[K: UnivEq, V]: At[ListMap[K, V], K, Option[V]] = {
      At(i =>
        Lens{ m: ListMap[K, V] => m.get(i) } { optV => map => optV.fold(map - i)(v => map + (i -> v)) }
      )
    }

  }

}
