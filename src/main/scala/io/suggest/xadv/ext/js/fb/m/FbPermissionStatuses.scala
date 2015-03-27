package io.suggest.xadv.ext.js.fb.m

import io.suggest.model.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 16:25
 * Description:
 * Модель состояний выставленных у юзера пермишенов.
 * Изначально было только два варианта: заапрувлено и отказано, т.е. как бы булево.
 */
object FbPermissionStatuses extends LightEnumeration {

  /** Интерфейс экземпляра модели. */
  protected trait ValT extends super.ValT {
    /** Строка по мнению фейсбука. */
    val fbName: String

    override def toString = fbName

    /** Дан ли допуск к указанному разрешению? */
    def isGranted: Boolean
  }

  /** Абстрактный экземпляр модели. */
  protected abstract sealed class Val(val fbName: String) extends ValT

  override type T = Val

  /** Юзер заапрувил разрешение. */
  val Granted: T = new Val("granted") {
    override def isGranted = true
  }

  /** Юзер вручную отключил разрешение. */
  val Declined: T = new Val("declined") {
    override def isGranted = false
  }

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Granted.fbName   => Some(Granted)
      case Declined.fbName  => Some(Declined)
      case _                => None   // TODO Логгировать неизвестные значения, желательно прямо на сервере.
    }
  }
}

