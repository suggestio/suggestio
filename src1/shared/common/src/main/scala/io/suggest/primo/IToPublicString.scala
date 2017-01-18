package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.17 16:43
  * Description: Интерфейс для аналога метода toString, но пригодный для рендера строк,
  * пригодных для публикации.
  *
  * Изначально появилось при рендере ошибок от adv-ext прямо в браузер. Они содержали
  * слишком подробные shell-команды.
  */
object IToPublicString {

  /** Извлечь публикуемую строку из произвольного инстанса. */
  def getPublicString(v: AnyRef): String = {
    v match {
      case tps: IToPublicString => tps.toPublicString
      case ex: Throwable        => ex.getMessage
      case other                => other.toString
    }
  }

}

trait IToPublicString {

  def toPublicString: String

}
