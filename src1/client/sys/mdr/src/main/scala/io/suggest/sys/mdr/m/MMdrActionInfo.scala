package io.suggest.sys.mdr.m

import diode.FastEq
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemType
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.18 16:42
  * Description: Нагрузка для экшена модерации, связанной с элементом, типом item'а, конкретным item'ом и т.д.
  */
object MMdrActionInfo {

  implicit object MMdrActionInfoFastEq extends FastEq[MMdrActionInfo] {
    override def eqv(a: MMdrActionInfo, b: MMdrActionInfo): Boolean = {
      (a.itemId ===* b.itemId) &&
      (a.itemType ===* b.itemType) &&
      (a.directSelfAll ==* b.directSelfAll) &&
      (a.directSelfId ===* b.directSelfId)
    }
  }

  implicit def univEq: UnivEq[MMdrActionInfo] = UnivEq.derive

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

