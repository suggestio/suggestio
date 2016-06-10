package io.suggest.sc

import io.suggest.common.menum.{EnumMaybeWithName, ILightEnumeration, LightEnumeration, StrIdValT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 17:40
 * Description: Модель назначений рендера выдачи. Выдача может рендериться на чужом сайте, на главной s.io,
 * на wifi captive portal, в контейнер мобильного приложения и т.д.
 * Назначения описывают возможные различия в отображении/поведении выдачи.
 */
object MScTargetIds {

  /** id основного назначения рендера выдачи. */
  def PRIMARY_ID = "a"

}


/** Базовая реализация модели назначений рендера выдачи. */
trait MScTargetsBaseT extends ILightEnumeration with StrIdValT {

  /** Интерфейс экземпляра будущих моделей. */
  protected trait ValT extends super.ValT {

    /** Доступна панель навигации по узлам рекламной сети? */
    def withAdnNav: Boolean

    /** Выдачу можно закрыть и смотреть сайт под ней? */
    def isCloseable : Boolean
  }

  /** Реализация флагов Primary-назначения выдачи. */
  protected[this] trait PrimaryValT extends ValT {
    override def withAdnNav   = true
    override def isCloseable  = false
  }

  override type T <: ValT

  /** Основной режим работы выдачи на главной s.io или где-то рядом. */
  val Primary: PrimaryValT

}


import MScTargetIds._


/** Реализация модели назначений [[MScTargetsBaseT]] через Enumeration. */
trait MScTargetsT extends EnumMaybeWithName with MScTargetsBaseT {

  abstract protected[this] class Val(val strId: String)
    extends super.Val(strId)
    with super.ValT

  override type T = Val

  override val Primary = new Val(PRIMARY_ID) with PrimaryValT

}


/** Максимально облегченная реализация модели назначений без Enumeration для scala-js. */
trait MScTargetsLightT extends MScTargetsBaseT with LightEnumeration {

  abstract protected[this] class Val(val strId: String) extends ValT

  override type T = Val

  override val Primary = new Val(PRIMARY_ID) with PrimaryValT

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Primary.strId  => Some(Primary)
      case _              => None
    }
  }

}

