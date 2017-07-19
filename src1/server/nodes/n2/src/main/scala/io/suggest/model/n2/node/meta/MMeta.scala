package io.suggest.model.n2.node.meta

import io.suggest.model.PrefixedFn
import io.suggest.model.n2.node.meta.colors.MColors
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.model.IGenEsMappingProps

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:16
 * Description: Модель метаданных одного узла графа N2.
 * Из-за множества полей, является над-моделью, которая уже содержит внутри себя
 * более конкретные поля.
 */

object MMeta extends IGenEsMappingProps {

  /** Названия ES-полей модели и подмоделей. */
  object Fields {

    object Basic extends PrefixedFn {
      val BASIC_FN            = "b"
      override protected def _PARENT_FN = BASIC_FN

      import MBasicMeta.{Fields => F}
      def NAME_SHORT_NOTOK_FN = _fullFn( F.NameShort.NAME_SHORT_NOTOK_FN )
      def DATE_CREATED_FN     = _fullFn( F.DATE_CREATED_FN )
    }

    object Person {
      val PERSON_FN     = "p"
    }

    object Address {
      val ADDRESS_FN    = "a"
    }

    object Business {
      val BUSINESS_FN   = "u"
    }

    object Colors {
      val COLORS_FN     = "c"
    }

  }


  import Fields.Basic.BASIC_FN
  import Fields.Person.PERSON_FN
  import Fields.Address.ADDRESS_FN
  import Fields.Business.BUSINESS_FN
  import Fields.Colors.COLORS_FN
  import MAddressEs.MADDRESS_FORMAT
  import MBusinessInfoEs.MBUSINESS_INFO_FORMAT

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MMeta] = (
    // На момент разработки (около 2015.sep.25) модели были в базе экземпляры без этих метаданных.
    // TODO Потом (после наверное 2015.nov) можно будет "Nullable" удалить у basic meta.
    (__ \ BASIC_FN).formatNullable[MBasicMeta]
      .inmap[MBasicMeta](
        opt2ImplEmpty1F( MBasicMeta() ),
        someF
      ) and
    (__ \ PERSON_FN).formatNullable[MPersonMeta]
      .inmap[MPersonMeta] (
        opt2ImplMEmptyF ( MPersonMeta ),
        implEmpty2OptF
      ) and
    (__ \ ADDRESS_FN).formatNullable[MAddress]
      .inmap [MAddress] (
        opt2ImplMEmptyF( MAddress ),
        implEmpty2OptF
      ) and
    (__ \ BUSINESS_FN).formatNullable[MBusinessInfo]
      .inmap[MBusinessInfo](
        opt2ImplMEmptyF( MBusinessInfo ),
        implEmpty2OptF
      ) and
    (__ \ COLORS_FN).formatNullable[MColors]
      .inmap[MColors](
        opt2ImplMEmptyF( MColors ),
        implEmpty2OptF
      )
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    // Сформировать анализируемые поля-объекты.
    val info = List[(String, IGenEsMappingProps)](
      (BASIC_FN,        MBasicMeta),
      (PERSON_FN,       MPersonMeta),
      (ADDRESS_FN,      MAddressEs),
      (BUSINESS_FN,     MBusinessInfoEs)
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
) {

  /** Вернуть инстанс модели [[MMetaPub]] на основе данной модели. */
  def public = MMetaPub(address, business)

}
