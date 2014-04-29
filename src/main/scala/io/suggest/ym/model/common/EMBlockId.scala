package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.JsNumber

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.14 10:00
 * Description: id блока, используемого для формирования выдачи рекламной карточки
 */
object EMBlockId {
  val BLOCK_ID_ESFN = "blockId"
}


import EMBlockId._

trait EMBlockIdStatic extends EsModelStaticT {
  override type T <: EMBlockIdMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldNumber(BLOCK_ID_ESFN, DocFieldTypes.integer, FieldIndexingVariants.not_analyzed, include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (BLOCK_ID_ESFN, blockIdRaw) =>
        acc.blockId = EsModel.intParser(blockIdRaw)
    }
  }
}


trait EMBlockId extends EsModelT {
  override type T <: EMBlockId
  def blockId: Int

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    BLOCK_ID_ESFN -> JsNumber(blockId) :: super.writeJsonFields(acc)
  }
}


trait EMBlockIdMut extends EMBlockId {
  override type T <: EMBlockIdMut
  var blockId: Int
}
