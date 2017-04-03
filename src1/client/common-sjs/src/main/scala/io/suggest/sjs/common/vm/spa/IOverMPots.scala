package io.suggest.sjs.common.vm.spa

import diode.data.{PendingBase, Pot}

/** Модель с case-class, содержащая в себе список [[IMPot]]'ов. */
trait IOverMPots extends Product {

  protected def _mPotsIterator: Iterator[IMPots] = {
    productIterator
      .flatMap {
        case opt: Option[_] =>
          opt.toList
        case seqs: TraversableOnce[_] =>
          seqs
        case x =>
          x :: Nil
      }
      .flatMap {
        case ipots: IMPots =>
          ipots :: Nil
        case _ =>
          Nil
      }
  }

  private def _firstPotRes[T](f: Pot[_] => TraversableOnce[T]): Option[T] = {
    _mPotsIterator
      .flatMap(_._pots)
      .flatMap(f)
      .toStream
      .headOption
  }


  def firstPotFailed: Option[Throwable] = {
    _firstPotRes {
      _.exceptionOption
    }
  }

  def firstPotPending: Option[Long] = {
    _firstPotRes { pot =>
      if (pot.isPending) {
        val st = pot.asInstanceOf[PendingBase].startTime
        st :: Nil
      } else {
        Nil
      }
    }
  }

}
