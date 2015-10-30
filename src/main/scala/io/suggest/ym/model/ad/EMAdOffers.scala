package io.suggest.ym.model.ad

import io.suggest.model.es.{EsModelStaticMutAkvT, EsModelPlayJsonT, EsModelUtil}
import java.{util => ju}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import EsModelUtil.FieldsJsonAcc
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 19:04
 * Description: Поле рекламных офферов, лежащих внутри рекламных карточек.
 */
object EMAdOffers {

  /** Название поля, в котором складируются офферы. */
  val OFFERS_ESFN       = "offers"

  /** Имя поля с телом оффера. Нужно т.к. тип оффера хранится отдельно от оффера. */
  val OFFER_BODY_ESFN   = "offerBody"

  /** В списке офферов порядок поддерживается с помощью поля n, которое поддерживает порядок по возрастанию. */
  val N_ESFN            = "n"

}


import EMAdOffers._


trait EMAdOffersStatic extends EsModelStaticMutAkvT {

  override type T <: EMAdOffersMut

  abstract override def generateMappingProps: List[DocField] = {
    // Полный маппинг для поля offer.
    val offersField = FieldNestedObject(OFFERS_ESFN,
      enabled = true,
      //includeInRoot = true,
      properties = Seq(
        FieldNumber(N_ESFN, index = FieldIndexingVariants.no, include_in_all = false, fieldType = DocFieldTypes.integer),
        FieldObject(OFFER_BODY_ESFN, enabled = true, properties = AOBlock.generateMappingProps)
      )
    )
    // Закинуть результат в аккамулятор.
    offersField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (OFFERS_ESFN, value: java.lang.Iterable[_]) =>
        acc.offers = value.toList
          .map(AdOffer.deserializeOne)
          .sortBy(_.n)
    }
  }

}


/** Интерфейс поля offers. Вынесен их [[EMAdOffers]] из-за потребностей blocks-инфраструктуры. */
trait IOffers {
  def offers: List[AOBlock]
}

trait EMAdOffersI extends EsModelPlayJsonT with IOffers {
  override type T <: EMAdOffersI
}

/** read-only аддон для экземпляра [[EsModelPlayJsonT]] для добавления поддержки работы с полем offers. */
trait EMAdOffers extends EMAdOffersI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (offers.nonEmpty) {
      val offersJson = offers.map(_.renderPlayJson)
      (OFFERS_ESFN, JsArray(offersJson)) :: acc0
    } else {
      acc0
    }
  }

}


trait EMAdOffersMut extends EMAdOffers {
  override type T <: EMAdOffersMut
  var offers: List[AOBlock]
}


// -------------- Далее идёт конструктор, из которого собираются офферы ---------------
object AdOffer {

  /** Десериализовать один оффер. */
  def deserializeOne(x: Any): AOBlock = {
    x match {
      case jsObject: ju.Map[_, _] =>
        val n: Int = Option(jsObject get N_ESFN)
          .map(EsModelUtil.intParser)
          .getOrElse(0)
        val offerBody = jsObject.get(OFFER_BODY_ESFN)
        AOBlock.deserializeBody(offerBody, n)
    }
  }

}

/** Абстрактный оффер. */
trait AdOfferT extends Serializable {

  /** Порядковый номер оффера в списке офферов. Нужен для поддержания исходного порядка. */
  def n: Int

  @JsonIgnore
  def renderPlayJson = {
    // Метаданные оффера содержат его порядковый номер и тип. Body содержит сами данные по офферу.
    JsObject(Seq(
      N_ESFN          -> JsNumber(n),
      OFFER_BODY_ESFN -> JsObject(renderPlayJsonBody)
    ))
  }

  @JsonIgnore
  def renderPlayJsonBody: FieldsJsonAcc
}



