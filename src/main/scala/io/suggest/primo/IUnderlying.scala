package io.suggest.primo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 10:30
 * Description: Интерфейс для врапперов, использующих поле с именем _underlying.
 */
trait IUnderlying {

  /** Заворачиваемый во враппер экземпляр. */
  def _underlying: Any

}
