package io.suggest.xadv.ext.js.fb.m

import io.suggest.model.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.03.15 10:08
 * Description: Режимы аутентификаций, передаваемые в FB.login().
 */
object FbAuthTypes extends LightEnumeration {

  /** Интерфейс экземпляра модели. */
  protected sealed trait ValT extends super.ValT {
    val fbType: String
    override def toString = fbType
  }

  /** Класс экземпляра модели. */
  protected sealed class Val(val fbType: String) extends ValT {
    override def equals(obj: scala.Any): Boolean = {
      super.equals(obj) || {
        obj match {
          case v: Val => v.fbType == this.fbType
          case _ => false
        }
      }
    }
  }

  override type T = Val

  /** Use this when re-requesting a declined permission. */
  val ReRequest: T = new Val("rerequest")

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case ReRequest.fbType => Some(ReRequest)
      case _                => None
    }
  }

}
