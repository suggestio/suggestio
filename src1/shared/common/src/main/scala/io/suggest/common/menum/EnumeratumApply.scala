package io.suggest.common.menum

import enumeratum.{Enum, EnumEntry}
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 12:51
  * Description: Аналог [[EnumApply]] для enumeratum.
  */
trait EnumeratumApply[T <: EnumEntry with IStrId] extends Enum[T] {

  //@deprecated("Use withName instead", "2017.apr.5")
  //def withNameT(name: String): T = {
  //  withName(name)
  //}

  def unapply(x: T): Option[String] = {
    Some(x.strId)
  }

  def onlyIds(input: TraversableOnce[T]): Iterator[String] = {
    input.toIterator.map(_.strId)
  }

}
