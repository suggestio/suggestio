package io.suggest.model.menum

import io.suggest.common.menum.EnumValue2Val
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 17:03
 * Description: Поддержка json reads для play.json десериализации.
 */
trait EnumJsonReadsT extends EnumValue2Val {

  implicit def reads: Reads[T] = {
    __.read[String]
      .map(withName)
  }

}

trait EnumJsonReadsValT extends EnumJsonReadsT {
  override implicit val reads = super.reads
}
