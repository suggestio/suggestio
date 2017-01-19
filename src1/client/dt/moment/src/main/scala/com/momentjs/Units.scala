package com.momentjs

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 9:33
  * Description: Model of units of momentjs's time.
  */
sealed trait IIndexed {
  def index: Int
}

sealed trait IUnit {
  def singular: String = toString
  def plural: String = singular + "s"
  def short: String = singular.substring(0, 1)
}

sealed protected trait ShortUpperCase extends IUnit {
  override def short = super.short.toUpperCase()
}

object Units {

  case object year extends IUnit with IIndexed {
    override def index = 0
  }

  case object quarter extends ShortUpperCase

  case object month extends ShortUpperCase with IIndexed {
    override def index = 1
  }

  case object week extends IUnit

  case object date extends ShortUpperCase with IIndexed {
    override def index = 2
  }

  case object day extends ShortUpperCase

  case object hour extends IUnit with IIndexed {
    override def index = 3
  }

  case object minute extends IUnit with IIndexed {
    override def index = 4
  }

  case object second extends IUnit with IIndexed {
    override def index = 5
  }

  case object milliSecond extends IUnit with IIndexed {
    override def index = 6
  }


  implicit def unit2index(unit: IIndexed): Int = {
    unit.index
  }

  implicit def unit2name(unit: IUnit): String = {
    unit.singular
  }

}
