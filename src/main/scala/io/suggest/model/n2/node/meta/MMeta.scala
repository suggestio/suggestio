package io.suggest.model.n2.node.meta

import io.suggest.common.EmptyProduct
import io.suggest.model.IGenEsMappingProps
import io.suggest.model.n2.node.meta.colors.MColors
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:16
 * Description: Модель метаданных одного узла графа N2.
 * Из-за множества полей, является над-моделью, которая уже содержит внутри себя
 * более конкретные поля.
 */

object MMeta extends IGenEsMappingProps {

  val BASIC_FN        = "b"
  val PERSON_FN       = "p"
  val ADDRESS_FN      = "a"
  val BUSINESS_FN     = "u"
  val COLORS_FN       = "c"

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MMeta] = (
    // На момент разработки (около 2015.sep.25) модели были в базе экземпляры без этих метаданных.
    // TODO Потом (после наверное 2015.nov) можно будет "Nullable" удалить у basic meta.
    (__ \ BASIC_FN).formatNullable[MBasicMeta]
      .inmap[MBasicMeta](
        _ getOrElse MBasicMeta(),
        Some.apply
      ) and
    (__ \ PERSON_FN).formatNullable[MPersonMeta]
      .inmap[MPersonMeta] (
        _ getOrElse MPersonMeta.empty,
        product2emptyOpt
      ) and
    (__ \ ADDRESS_FN).formatNullable[MAddress]
      .inmap [MAddress] (
        _ getOrElse MAddress.empty,
        product2emptyOpt
      ) and
    (__ \ BUSINESS_FN).formatNullable[MBusinessInfo]
      .inmap[MBusinessInfo](
        _ getOrElse MBusinessInfo.empty,
        product2emptyOpt
      ) and
    (__ \ COLORS_FN).formatNullable[MColors]
      .inmap[MColors](
        _ getOrElse MColors.empty,
        product2emptyOpt
      )
  )(apply, unlift(unapply))

  /** Конвертация возможно пустой подмодели в опциональное значение подмодели. */
  private def product2emptyOpt[T <: EmptyProduct](product: T): Option[T] = {
    if (product.nonEmpty)
      Some(product)
    else
      None
  }

  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    // Сформировать анализируемые поля-объекты.
    val info = List(
      (BASIC_FN,      MBasicMeta),
      (PERSON_FN,     MPersonMeta),
      (ADDRESS_FN,    MAddress),
      (BUSINESS_FN,   MBusinessInfo)
    )
    val acc0 = for ((fn, model) <- info) yield {
      FieldObject(fn, enabled = true, properties = model.generateMappingProps)
    }
    // Добавить неанализируемые поля.
    FieldObject(COLORS_FN, enabled = false, properties = Nil) ::
      acc0
  }

}


/** Дичайшая убер-модель метаданных, например. */
case class MMeta(
  basic         : MBasicMeta,
  person        : MPersonMeta     = MPersonMeta.empty,
  address       : MAddress        = MAddress.empty,
  business      : MBusinessInfo   = MBusinessInfo.empty,
  colors        : MColors         = MColors.empty
)
