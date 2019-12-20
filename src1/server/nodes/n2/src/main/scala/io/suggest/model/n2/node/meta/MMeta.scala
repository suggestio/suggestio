package io.suggest.model.n2.node.meta

import io.suggest.color.MColors
import io.suggest.model.PrefixedFn
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.EmptyUtil._
import io.suggest.es.{IEsMappingProps, MappingDsl}
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:16
 * Description: Модель метаданных одного узла графа N2.
 * Из-за множества полей, является над-моделью, которая уже содержит внутри себя
 * более конкретные поля.
 */

object MMeta extends IEsMappingProps {

  /** Названия ES-полей модели и подмоделей. */
  object Fields {

    object Basic extends PrefixedFn {
      val BASIC_FN            = "b"
      override protected def _PARENT_FN = BASIC_FN

      import MBasicMeta.{Fields => F}
      def BASIC_NAME_FN       = _fullFn( F.NAME_FN )
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

    object Colors extends PrefixedFn {
      val COLORS_FN     = "c"
      override protected def _PARENT_FN = COLORS_FN

      def BG_CODE_FN = _fullFn( MColors.Fields.Bg.BG_CODE_FN )
      def FG_CODE_FN = _fullFn( MColors.Fields.Fg.FG_CODE_FN )
    }

  }


  import Fields.Basic.BASIC_FN
  import Fields.Person.PERSON_FN
  import Fields.Address.ADDRESS_FN
  import Fields.Business.BUSINESS_FN
  import Fields.Colors.COLORS_FN

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


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._

    val info = List[(String, IEsMappingProps)](
      BASIC_FN -> MBasicMeta,
      PERSON_FN -> MPersonMeta,
      ADDRESS_FN -> MAddress,
      BUSINESS_FN -> MBusinessInfo,
    )
      .esSubModelsJsObjects( nested = false )

    info ++ Json.obj(
      COLORS_FN -> FObject.plain( someFalse ),
    )
  }


  val basic     = GenLens[MMeta](_.basic)
  val person    = GenLens[MMeta](_.person)
  val address   = GenLens[MMeta](_.address)
  val business  = GenLens[MMeta](_.business)
  val colors    = GenLens[MMeta](_.colors)

}


/** Дичайшая убер-модель метаданных, например. */
case class MMeta(
                  basic         : MBasicMeta      = MBasicMeta(),
                  person        : MPersonMeta     = MPersonMeta.empty,
                  address       : MAddress        = MAddress.empty,
                  business      : MBusinessInfo   = MBusinessInfo.empty,
                  colors        : MColors         = MColors.empty
                ) {

  /** Вернуть инстанс модели [[MMetaPub]] на основе данной модели. */
  def public = MMetaPub(basic.name, address, business, colors)

  /** Залить данные из MMetaPub в эту модель. */
  def withPublic(metaPub: MMetaPub): MMeta = {
    copy(
      basic = basic.withNameOpt(
        nameOpt = Option( metaPub.name )
      ),
      address  = metaPub.address,
      business = metaPub.business,
      colors   = metaPub.colors
    )
  }

}
