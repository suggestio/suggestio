package io.suggest.ym.model.common

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.model.sc.common.SlNameTokenStr

import scala.collection.immutable.SortedSet

/** Выходы узла для отображения рекламных карточек. */
object AdnSinks extends EnumMaybeWithName with EnumJsonReadsValT {

  protected abstract class Val(val name: String) extends super.Val(name) with SlNameTokenStr {
    def longName: String
    def sioComissionDflt: Float
  }
  override type T = Val

  val SINK_WIFI: T = new Val("w") {
    override def longName = "wifi"
    override def sioComissionDflt = 0.30F
  }

  val SINK_GEO: T = new Val("g") {
    override def longName: String = "geo"
    override def sioComissionDflt = 1.0F
  }

  def ordered: Seq[T] = {
    values
      .foldLeft( List.empty[T] ) { (acc, e) => e :: acc }
      .sortBy(_.longName)
  }

  def default = SINK_GEO

  def maybeWithLongName(ln: String): Option[T] = {
    values
      .find(_.longName == ln)
      .asInstanceOf[Option[T]]
  }

}
