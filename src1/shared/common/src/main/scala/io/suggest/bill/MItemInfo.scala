package io.suggest.bill

import boopickle.Default._
import io.suggest.mbill2.m.item.typ.MItemType

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 10:46
  * Description: Какая-то человеческая инфа по item'у биллинга.
  */
object MItemInfo {

  /** Поддержка boopickle-сериализации. */
  implicit val mItemInfoPickler: Pickler[MItemInfo] = {
    implicit val mPriceP    = MPrice.mPricePickler
    implicit val mItemTypeP = MItemType.mItemTypePickler
    implicit val mNameIdP   = MNameId.mNameIdPickler
    generatePickler[MItemInfo]
  }

}


/** Класс инфы по item'у.
  *
  * @param iType Тип item'а.
  * @param price Ценник.
  * @param rcvr Данные по узлу-ресиверу, если есть.
  * @param gsInfo Данные по гео-шейпу, если есть.
  */
case class MItemInfo(
                      iType   : MItemType,
                      price   : MPrice,
                      rcvr    : Option[MNameId],
                      gsInfo  : Option[String]
                    )


/** Вспомогательная модель очень базовой инфы по чему-то сложному: только опциональный id и название чего-то. */
object MNameId {
  /** Поддержка boopickle-сериализации. */
  implicit val mNameIdPickler: Pickler[MNameId] = {
    generatePickler[MNameId]
  }
}
/** Простая модель для отображения очень базовых данных по какому-то узлу. */
case class MNameId(
                    id    : Option[String],
                    name  : String
                  )
