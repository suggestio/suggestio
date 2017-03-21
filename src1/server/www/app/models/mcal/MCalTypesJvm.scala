package models.mcal

import io.suggest.cal.m.{MCalType, MCalTypes}
import io.suggest.common.empty.EmptyUtil
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 15:29
  * Description: Статическая модель "типов" календарей, т.е. статических локализаций оных.
  * Календарей может быть много, а локализации и прочее -- ограниченный набор.
  */
class MCalTypesJvm {

  /** Поддержка опционального маппинга из/в JSON. */
  implicit val mCalTypeOptFormat: Format[Option[MCalType]] = {
    __.format[String]
      // Чтобы не было экзепшенов, надо дергать maybeWithName() и смотреть значение Option.
      .inmap [Option[MCalType]] (
        MCalTypes.withNameOption,
        _.fold("")(_.strId)
      )
  }

  /** Поддержка обязательного маппинга из/в JSON. */
  implicit val mCalTypeFormat: Format[MCalType] = {
    val reads = mCalTypeOptFormat
      // TODO Хз как передать некорректное значение внутрь ValidationError... А надо бы, очень надо.
      .filter( ValidationError("error.unknown.name") )(_.nonEmpty)
      .map(_.get)
    val writes = (mCalTypeOptFormat: Writes[Option[MCalType]])
      .contramap[MCalType]( EmptyUtil.someF )
    Format(reads, writes)
  }


  /** Опциональный маппинг для формы. */
  def calTypeOptM: Mapping[Option[MCalType]] = {
    nonEmptyText(minLength = 1, maxLength = 10)
      .transform [Option[MCalType]] (MCalTypes.withNameOption, _.fold("")(_.strId))
  }

  /** Обязательный маппинг для формы. */
  def calTypeM: Mapping[MCalType] = {
    calTypeOptM
      .verifying("error.required", _.nonEmpty)
      .transform [MCalType] (EmptyUtil.getF, EmptyUtil.someF)
  }

}
