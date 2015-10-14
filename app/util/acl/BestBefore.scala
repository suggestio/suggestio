package util.acl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 15:42
 * Description: Поддержка валидации best-before на реквестах.
 * Это позволяет защититься от повторения этого реквеста в будущем.
 * Применяется совместно с цифровой подписью на аргументах запроса.
 */
object BestBefore {

  /** Текущее время в секундах. */
  def nowSec = System.currentTimeMillis() / 1000L

}
