package io.suggest.common.m.mad

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.16 16:36
  * Description: Модель интерфейса для поля madId, часто присутствующего в различных моделях.
  */

object IMadId {

  /** Сбор id карточек рядом с указанной.
    *
    * @param adId id искомой карточки.
    * @param q Исходная очередь карточек.
    * @param stepCount Макс.кол-во шагов в стороны.
    * @return Последовательности id в исходном порядке.
    */
  def nearIds(adId: String, q: TraversableOnce[IMadId], stepCount: Int): Seq[String] = {
    val l = q.toIterator
      .map(_.madId)
      .toSeq
    val i = l.indexOf(adId)
    l.slice(
      from  = Math.max(0, i - stepCount),
      until = i + stepCount
    )
  }


  /** Вернуть итератор карточек после указанной. */
  def adsAfter[T <: IMadId](adId: String, fads: TraversableOnce[T]): Iterator[T] = {
    fads.toIterator
      .dropWhile(_.madId != adId)
      .drop(1)
  }

  /** Возвращать итератор карточек до указанной. */
  def adsBefore[T <: IMadId](adId: String, fads: TraversableOnce[T]): Iterator[T] = {
    fads.toIterator
      .takeWhile(_.madId != adId)
  }

}


/** Интерфейс для поля с обязательнымы id карточки. */
trait IMadId {

  /** Обязательный id рекламной карточки. */
  def madId: String

}
