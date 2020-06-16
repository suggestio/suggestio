package io.suggest.id.token

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.06.19 15:11
  * Description: Модель ограничений на употребление id-токена.
  */
object MIdTokenConstaints extends IEmpty {

  override type T = MIdTokenConstaints

  override def empty = apply()

  implicit def mIdTokenConstaintsJson: OFormat[MIdTokenConstaints] = {
    (__ \ "p").formatNullable[Set[String]]
      .inmap[MIdTokenConstaints](apply, _.personIdsC)
  }

  @inline implicit def univEq: UnivEq[MIdTokenConstaints] = UnivEq.derive

  def personIdsC = GenLens[MIdTokenConstaints]( _.personIdsC )

}


/** Контейнер ограничений.
  *
  * @param personIdsC Ограничение на допустимые personId в сессии:
  *                   None - без ограничений.
  *                   Some([]) - анонимус
  *                   Some([a,b,...]) - юзер залогинен под одним из указанных id.
  */
case class MIdTokenConstaints(
                               personIdsC     : Option[Set[String]]         = None,
                             )
  extends EmptyProduct
