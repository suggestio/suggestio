package io.suggest.xadv.ext.js.vk.m

import io.suggest.model.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.15 16:14
 * Description: Модель пермишшенов вконтакта.
 */
object VkPerms extends LightEnumeration {

  /** Интерфейс экземпляра модели. */
  sealed protected trait ValT extends super.ValT {
    def name: String
    override def toString = name
    def toInt: Int

    override def equals(obj: scala.Any): Boolean = {
      super.equals(obj) || {
        obj match {
          case v: ValT  => v.toInt == this.toInt
          case _        => false
        }
      }
    }
  }

  override type T = ValT

  /** Доступ к стене. */
  val Wall = new ValT {
    override def name = "wall"
    override def toInt = 8192
  }

  /** Фотоальбомы и фотки. */
  val Photos = new ValT {
    override def name = "photos"
    override def toInt = 4
  }

  def values: Seq[T] = Seq(Wall, Photos)

  override def maybeWithName(n: String): Option[T] = {
    if (Wall.name == n) {
      Some(Wall)
    } else if (Photos.name == n) {
      Some(Photos)
    } else {
      None
    }
  }

  def toBitMask(perms: TraversableOnce[T]): Int = {
    perms.foldLeft(0)(_ + _.toInt)
  }
  def toBitMask(perms: T*): Int = {
    toBitMask(perms)
  }

  def fromBitMask(mask: Int): Seq[T] = {
    values.filter { perm =>
      val c = perm.toInt
      (mask & c) == c
    }
  }

}
