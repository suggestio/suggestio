package io.suggest.lk.tags.edit.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 11:47
  * Description: Модель одного найденного тега на сервере при поиске тегов.
  */
trait ITagFound {

  def name: String

  def count: Int

}
