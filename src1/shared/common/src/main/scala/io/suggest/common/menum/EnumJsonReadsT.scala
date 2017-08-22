package io.suggest.common.menum

import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 17:03
 * Description: Поддержка json reads для play.json десериализации.
 */

trait EnumJsonReadsT extends IMaybeWithName {

  implicit def reads: Reads[T] = {
    implicitly[Reads[String]]
      // Чтобы не было экзепшенов, надо дергать maybeWithName() и смотреть значение Option.
      .map(maybeWithName)
      // TODO Хз как передать некорректное значение внутрь ValidationError... А надо бы, очень надо.
      .filter( JsonValidationError("e.name.unknown") )(_.nonEmpty)
      .map(_.get)
  }

}

trait EnumJsonReadsValT extends EnumJsonReadsT {
  override implicit val reads = super.reads
}
