package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.{JsObject, JsNumber}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.14 10:00
 * Description: id блока, используемого для формирования выдачи рекламной карточки
 */
object EMBlockMeta {
  val BLOCK_META_ESFN = "blockMeta"
}


import EMBlockMeta._

trait EMBlockMetaStatic extends EsModelStaticMutAkvT {
  override type T <: EMBlockMetaMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(BLOCK_META_ESFN, enabled = false, properties = Nil) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (BLOCK_META_ESFN, blockMetaRaw) =>
        acc.blockMeta = BlockMeta.deserialize(blockMetaRaw)
    }
  }
}


/** Интерфейс blockId. Вынесена из [[EMBlockMeta]] из-за потребностей blocks-редактора и инфраструктуры. */
trait IBlockMeta {
  def blockMeta: BlockMeta
}

/** Интерфейсная часть EMBlockMeta. Вынесена, чтобы избежать сериализации поля blockMeta когда это не нужно. */
trait EMBlockMetaI extends EsModelPlayJsonT with IBlockMeta {
  override type T <: EMBlockMetaI
}

/** Аддон для экземпляра [[io.suggest.model.EsModelPlayJsonT]] для интеграции поля blockId в модель. */
trait EMBlockMeta extends EMBlockMetaI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    BLOCK_META_ESFN -> blockMeta.toPlayJson :: super.writeJsonFields(acc)
  }
}


trait EMBlockMetaMut extends EMBlockMeta {
  override type T <: EMBlockMetaMut
  var blockMeta: BlockMeta
}



object BlockMeta {

  val BLOCK_ID_ESFN = "blockId"
  val HEIGHT_ESFN = "height"

  /** Десериализация BlockMeta из выхлопа jackson. */
  def deserialize(x: Any): BlockMeta = {
    x match {
      case m: java.util.Map[_,_] =>
        BlockMeta(
          blockId = EsModel.intParser(m.get(BLOCK_ID_ESFN)),
          height  = EsModel.intParser(m.get(HEIGHT_ESFN))
        )
    }
  }

}

import BlockMeta._

/**
 * Представление глобальных данных блоков.
 * @param blockId id шаблона блока.
 * @param height высота блока.
 */
case class BlockMeta(
  var blockId: Int,
  var height: Int
) {
  /** Сериализация экземпляра этого класса в json-объект. */
  def toPlayJson = JsObject(Seq(
    BLOCK_ID_ESFN -> JsNumber(blockId),
    HEIGHT_ESFN   -> JsNumber(height)
  ))
}

