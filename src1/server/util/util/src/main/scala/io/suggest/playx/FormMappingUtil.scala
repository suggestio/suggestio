package io.suggest.playx

import io.suggest.common.empty.EmptyUtil
import play.api.data.Mapping

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 18:07
  * Description: Утиль для play from mappings.
  */
object FormMappingUtil {

  /** Сделать из опционального маппинга обязательный, путём проверки и раскрытия Option[X] в X.
    *
    * @param mapping Исходный опциональный маппинг.
    * @tparam T Тип маппящегося значения.
    * @return Обязательный маппинг.
    */
  def optMapping2required[T](mapping: Mapping[Option[T]]): Mapping[T] = {
    mapping
      .verifying("error.required", _.isDefined)
      .transform[T](EmptyUtil.getF, EmptyUtil.someF)
  }

}
