package models.mcal

import io.suggest.cal.m.{MCalType, MCalTypes}
import io.suggest.common.empty.EmptyUtil
import play.api.data.Forms._
import play.api.data.Mapping

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 15:29
  * Description: Статическая модель "типов" календарей, т.е. статических локализаций оных.
  * Календарей может быть много, а локализации и прочее -- ограниченный набор.
  */
class MCalTypesJvm {

  // TODO Подхватить маппинги из EnumeratumJvmUtil.

  /** Опциональный маппинг для формы. */
  def calTypeOptM: Mapping[Option[MCalType]] = {
    nonEmptyText(minLength = 1, maxLength = 10)
      .transform [Option[MCalType]] (
        MCalTypes.withValueOpt,
        _.fold("")(_.value)
      )
  }

  /** Обязательный маппинг для формы. */
  def calTypeM: Mapping[MCalType] = {
    calTypeOptM
      .verifying("error.required", _.nonEmpty)
      .transform [MCalType] (EmptyUtil.getF, EmptyUtil.someF)
  }

}
