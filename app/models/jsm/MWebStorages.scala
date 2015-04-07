package models.jsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 17:47
 * Description: WebStorage API совместимые хранилища, доступные в браузере.
 */
object MWebStorages extends Enumeration {

  type T = Value

  /** Постоянное хранилище браузера. */
  val LocalStorage = Value("localStorage")

  /** Хранилка данных до закрытия браузера. */
  lazy val SessionStorage = Value("sessionStorage")

}
