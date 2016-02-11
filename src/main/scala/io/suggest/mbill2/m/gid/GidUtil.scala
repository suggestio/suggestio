package io.suggest.mbill2.m.gid

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 12:58
  * Description: Утиль для работы с id'шниками.
  */
class GidUtil {

  /** Скомпилировать список элементов в карту оных по id.
    *
    * @param els Исходный список элементов.
    * @tparam T Тип каждого элемента в коллекции.
    * @return Карта элементов по их id.
    */
  def elements2map[T <: IGid](els: TraversableOnce[T]): Map[Gid_t, T] = {
    val iter = for {
      res   <- els.toIterator
      id    <- res.id
    } yield {
      id -> res
    }
    iter.toMap
  }

}

trait IGidUtilDi {
  def gidUtil: GidUtil
}
