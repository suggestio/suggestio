package models

import MPersonIdent.IdTypes.MPersonIdentType
import io.suggest.model.{EsModelStaticT, EsModel, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import EsModel._
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.03.14 16:50
 * Description: ES-Модель для работы с идентификациями юзеров.
 * Нужно для возможности юзеров логинится по-разному: persona, просто имя-пароль и т.д.
 * В suggest.io исторически была только persona, которая жила прямо в MPerson.
 * Все PersonIdent имеют общий формат, однако хранятся в разных типах в рамках одного индекса.
 */
object MPersonIdent {

  def generateMapping(typ: String): XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = typ,
      static_fields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = false, analyzer = FTS_RU_AN),
        FieldId(path = KEY_ESFN)  // Для надежной защиты от двойных добавлений.
      ),
      properties = Seq(
        FieldString(
          id = PERSON_ID_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = KEY_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = VALUE_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = IS_VERIFIED_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        )
      )
    )
  }

  /** Типы поддерживаемых алгоритмов идентификаций. В базу пока не сохраняются. */
  object IdTypes extends Enumeration {
    type MPersonIdentType = Value
    val MOZ_PERSONA       = Value
  }

}

import MPersonIdent._

trait MPersonIdent[E <: MPersonIdent[E]] extends EsModelT[E] {
  def personId: String
  def idType: MPersonIdentType
  def key: String
  def value: Option[String]
  def isVerified: Boolean

  /** Определяется реализацией: надо ли записывать в хранилище значение isVerified. */
  def writeVerifyInfo: Boolean

  def writeJsonFields(acc: XContentBuilder) {
    acc
      .field(PERSON_ID_ESFN, personId)
      .field(KEY_ESFN, key)
    if (value.isDefined)
      acc.field(VALUE_ESFN, value.get)
    if (writeVerifyInfo)
      acc.field(IS_VERIFIED_ESFN, isVerified)
  }
}


/** Идентификации от mozilla-persona. */
object MozillaPersonaIdent extends EsModelStaticT[MozillaPersonaIdent] {

  val ES_TYPE_NAME = "mpiMozPersona"

  def generateMapping = MPersonIdent.generateMapping(ES_TYPE_NAME)

  def applyKeyValue(acc: MozillaPersonaIdent): PartialFunction[(String, AnyRef), Unit] = {
    case (KEY_ESFN, value)        => acc.email = stringParser(value)
    case (PERSON_ID_ESFN, value)  => acc.personId = stringParser(value)
  }

  protected def dummy(id: String) = MozillaPersonaIdent(email=null, personId=null)

}

case class MozillaPersonaIdent(
  var email     : String,
  var personId  : String
) extends MPersonIdent[MozillaPersonaIdent] with MPersonLinks {
  /** Сгенерить id. Если допустить, что тут None, то _id будет из взят из поля key согласно маппингу. */
  def id: Option[String] = Some(email)
  def key = email
  def idType = IdTypes.MOZ_PERSONA
  def value = None
  def isVerified = true
  def writeVerifyInfo: Boolean = false
  def companion = MozillaPersonaIdent
}


