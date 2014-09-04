package io.suggest.ym.model.common

import io.suggest.model.{EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import play.api.libs.json._
import com.fasterxml.jackson.annotation.JsonIgnore
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.14 18:05
 * Description: Поле "причина отключения", используется для хранения причины бана в рекламной карточке.
 */

object EMDisableReason {
  val DISABLE_REASON_ESFN = "dsblReason"

  val ADN_ID_ESFN = "adnId"
  val REASON_ESFN = "reason"
}

import EMDisableReason._

trait EMDisableReasonStatic extends EsModelStaticMutAkvT {
  override type T <: EMDisableReasonMut

  abstract override def generateMappingProps: List[DocField] = {
    val drField = FieldObject(DISABLE_REASON_ESFN, enabled = false, properties = Nil)
    drField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (DISABLE_REASON_ESFN, value) =>
        acc.disableReason = value match {
          case null =>
            Nil
          // Начальная реализация умела только одну анонимную причину хранить из всех жалоб.
          case s: String =>
            Nil
          case l: jl.Iterable[_] if l.isEmpty =>
            Nil
          case _ =>
            JacksonWrapper.convert[List[DisableReason]](value)
        }
    }
  }
}


trait EMDisableReasonI extends EsModelPlayJsonT {
  override type T <: EMDisableReasonI

  /** Кто является изготовителем этой рекламной карточки? */
  def disableReason: List[DisableReason]
}


trait EMDisableReason extends EMDisableReasonI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (!disableReason.isEmpty) {
      val v = JsArray(disableReason.map(_.toPlayJson))
      DISABLE_REASON_ESFN -> v  ::  acc0
    } else {
      acc0
    }
  }

}

trait EMDisableReasonMut extends EMDisableReason {
  override type T <: EMDisableReasonMut
  var disableReason: List[DisableReason]
}


object DisableReason {

  def deserializeMap(jm: ju.Map[_,_]): DisableReason = {
    DisableReason(
      adnId  = Option(jm.get(ADN_ID_ESFN)).map(stringParser).getOrElse(""),
      reason = Option(jm.get(REASON_ESFN)).map(stringParser).getOrElse("")
    )
  }

}

/** Класс для представления причины выпиливания карточки. */
case class DisableReason(adnId: String, reason: String) {

  /** Сериализация этого класса в JSON. */
  @JsonIgnore
  def toPlayJson: JsObject = {
    JsObject(Seq(
      ADN_ID_ESFN -> JsString(adnId),
      REASON_ESFN -> JsString(reason)
    ))
  }

}
