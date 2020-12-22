package io.suggest.lk.tags.edit

import com.softwaremill.macwire._
import io.suggest.lk.tags.edit.r._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2020 17:36
  * Description: Поддержка compile-time DI для редактирования тегов.
  */
final class TagsEditModule {

  import io.suggest.ReactCommonModule._

  lazy val tagsEditR = wire[TagsEditR]

}
