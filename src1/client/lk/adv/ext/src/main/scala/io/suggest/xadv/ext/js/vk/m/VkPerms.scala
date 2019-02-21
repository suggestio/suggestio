package io.suggest.xadv.ext.js.vk.m

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 16:14
 * Description: Модель пермишшенов вконтакта.
 */
object VkPerms extends Enum[VkPerm] {

  /** Доступ к стене. */
  case object Wall extends VkPerm {
    override def name = "wall"
    override def toInt = 8192
  }

  /** Фотоальбомы и фотки. */
  case object Photos extends VkPerm {
    override def name = "photos"
    override def toInt = 4
  }


  override def values = findValues

  def fromBitMask(mask: Int): Seq[VkPerm] = {
    values.filter { perm =>
      val c = perm.toInt
      (mask & c) == c
    }
  }

}

sealed abstract class VkPerm extends EnumEntry {

  def name: String
  def toInt: Int

  override final def toString = name

}


object VkPerm {

  @inline implicit def univEq: UnivEq[VkPerm] = UnivEq.derive

  def toBitMask(perms: TraversableOnce[VkPerm]): Int = {
    perms.foldLeft(0)(_ + _.toInt)
  }

}
