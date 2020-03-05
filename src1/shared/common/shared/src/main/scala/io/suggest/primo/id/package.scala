package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.03.2020 11:20
  */
package object id {

  implicit final class OptIdOpsExt[T]( private val els: IterableOnce[T] ) extends AnyVal {

    def kvByIdIter[K, R](kt2r: (K,T) => R)(implicit toIdOpt: T => Option[K]): Iterator[R] = {
      for {
        el <- els.iterator
        id <- toIdOpt(el)
      } yield {
        kt2r(id, el)
      }
    }

    def toIdIter[K](implicit toIdOpt: T => Option[K]): Iterator[K] = {
      kvByIdIter[K, K] { (k,_) => k }
    }

    /** Сборка итератора id -> T. */
    def zipWithIdIter[K](implicit toIdOpt: T => Option[K]): Iterator[(K, T)] =
      mapZipWithIdIter[K, T]( identity )

    def mapZipWithIdIter[K, V](modF: T => V)
                              (implicit toIdOpt: T => Option[K]): Iterator[(K, V)] = {
      kvByIdIter[K, (K,V)] { (k,t) => (k, modF(t)) }
    }

  }

}
