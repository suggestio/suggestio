package io.suggest.bill.price.dsl

import boopickle.Default._
import io.suggest.bill.MNameId
import io.suggest.geo.MGeoCircle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.17 15:12
  * Description: Модель описания причины тарификации, увеличения тарифа или чего-то в этом роде.
  */
object MPriceReason {

  /** Поддержка бинарной сериализации между клиентом и сервером. */
  implicit val iPriceReasonPickler: Pickler[MPriceReason] = {
    implicit val mReasonTypeP = MReasonType.mReasonTypePickler
    implicit val mGeoCircleP = MGeoCircle.mGeoCirlePickler
    implicit val mNameIdP = MNameId.mNameIdPickler

    generatePickler[MPriceReason]
  }

}


/** Класс описания причины с опциональными дополнительными данными.
  *
  * @param reasonType Тип причины по каталогу причин.
  * @param ints Целочисленный payload'ы, если есть.
  * @param strings Строковой payload'ы, если есть.
  * @param doubles Десятичные payload'ы, если есть.
  * @param geoCircles Гео-круги, если есть.
  */
final case class MPriceReason(
                               reasonType   : MReasonType,
                               ints         : Seq[Int]            = Nil,
                               strings      : Seq[String]         = Nil,
                               doubles      : Seq[Double]         = Nil,
                               geoCircles   : Seq[MGeoCircle]     = Nil,
                               nameIds      : Seq[MNameId]        = Nil
                             )