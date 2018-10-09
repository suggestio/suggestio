package io.suggest.sys.mdr

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptJsonFormatOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 16:42
  * Description: Нагрузка для экшена модерации, связанной с элементом, типом item'а, конкретным item'ом и т.д.
  */
object MMdrActionInfo {

  object Fields {
    val ITEM_ID_FN = "i"
    val ITEM_TYPE_FN = "t"
    val DIRECT_SELF_ALL_FN = "d"
    val DIRECT_SELF_ID_FN = "s"
  }


  implicit object MMdrActionInfoFastEq extends FastEq[MMdrActionInfo] {
    override def eqv(a: MMdrActionInfo, b: MMdrActionInfo): Boolean = {
      (a.itemId ===* b.itemId) &&
      (a.itemType ===* b.itemType) &&
      (a.directSelfAll ==* b.directSelfAll) &&
      (a.directSelfId ===* b.directSelfId)
    }
  }

  implicit def univEq: UnivEq[MMdrActionInfo] = UnivEq.derive


  /** Поддержка play-json. */
  implicit def mMdrActionInfoFormat: OFormat[MMdrActionInfo] = {
    val F = Fields
    (
      (__ \ F.ITEM_ID_FN).formatNullable[Gid_t] and
      (__ \ F.ITEM_TYPE_FN).formatNullable[MItemType] and
      (__ \ F.DIRECT_SELF_ALL_FN).formatNullable[Boolean].formatBooleanOrFalse and
      (__ \ F.DIRECT_SELF_ID_FN).formatNullable[String]
    )(apply, unlift(unapply))
  }

}


/** Модель базовой инфы по модерации.
  *
  * @param itemId id item, если модерация конкретного item'а.
  * @param itemType Тип item'ов, если модерация уровня группы.
  * @param directSelfId Бесплатное размещение на своём узле (id целевого узла).
  */
case class MMdrActionInfo(
                           itemId        : Option[Gid_t]        = None,
                           itemType      : Option[MItemType]    = None,
                           directSelfAll : Boolean              = false,
                           directSelfId  : Option[String]       = None,
                         )
  extends EmptyProduct
{

  override protected[this] def _nonEmptyValue(v: Any): Boolean = {
    v match {
      case x: Boolean => x
      case _ => super._nonEmptyValue(v)
    }
  }

}

