package io.suggest.mbill2.m.gid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:37
 * Description: Интерфейс для инстансов реализаций моделей, связанных с gid.
 */
trait IGid {

  def id: Option[Long]

}
