package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.{JsBoolean, JsObject, JsNumber}
import io.suggest.util.MyConfig.CONFIG

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.14 10:00
 * Description: id блока, используемого для формирования выдачи рекламной карточки
 */
object EMBlockMeta {
  val BLOCK_META_ESFN = "bm"
}


import EMBlockMeta._

trait EMBlockMetaStatic extends EsModelStaticMutAkvT {
  override type T <: EMBlockMetaMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(BLOCK_META_ESFN, enabled = true, properties = BlockMeta.generateMappingProps) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      // 2014.oct.14: Раньше был просто blockMeta и оно не индексировалось вообще никак. Но понадобилось это дело исправить
      // TODO Удалить поддержку ключа blockMeta вместе с этим комментом через какое-то время.
      case ((BLOCK_META_ESFN | "blockMeta"), blockMetaRaw) =>
        acc.blockMeta = BlockMeta.deserialize(blockMetaRaw)
    }
  }
}


/** Интерфейс blockId. Вынесена из [[EMBlockMeta]] из-за потребностей blocks-редактора и инфраструктуры. */
trait IEMBlockMeta {
  def blockMeta: BlockMeta
}

/** Интерфейсная часть EMBlockMeta. Вынесена, чтобы избежать сериализации поля blockMeta когда это не нужно. */
trait EMBlockMetaI extends EsModelPlayJsonT with IEMBlockMeta {
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
  val HEIGHT_ESFN   = "height"
  val WIDTH_ESFN    = "width"
  val WIDE_ESFN     = "wide"

  /** Поле ширины долго жило в настройках блока, а когда пришло время переезжать, возникла проблема с дефолтовым значением. */
  val WIDTH_DFLT = CONFIG.getInt("block.meta.width.dflt.px") getOrElse 300

  /** Десериализация BlockMeta из выхлопа jackson. */
  def deserialize(x: Any): BlockMeta = {
    x match {
      case m: java.util.Map[_,_] =>
        BlockMeta(
          blockId = EsModel.intParser(m get BLOCK_ID_ESFN),
          height  = EsModel.intParser(m get HEIGHT_ESFN),
          width   = Option(m get WIDTH_ESFN).fold(WIDTH_DFLT)(EsModel.intParser),
          wide    = Option(m get WIDE_ESFN).fold(false)(EsModel.booleanParser)
        )
    }
  }

  private def fint(fn: String) = {
    FieldNumber(fn, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  }

  def generateMappingProps: List[DocField] = {
    List(
      fint(BLOCK_ID_ESFN),
      fint(HEIGHT_ESFN),
      fint(WIDTH_ESFN),
      FieldBoolean(WIDE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

  val DEFAULT = BlockMeta(blockId = 20, height = 300, width = WIDTH_DFLT)

}


import BlockMeta._


/** Интерфейс для экземпляром BlockMeta. */
trait IBlockMeta extends MImgSizeT {
  /** id шаблона блока. */
  def blockId: Int
  /** Использовать широкое отображение? */
  def wide: Boolean
}

/**
 * Неизменяемое представление глобальных парамеров блока.
 * @param blockId
 * @param height высота блока.
 */
case class BlockMeta(
  blockId : Int,
  height  : Int,
  width   : Int,
  wide    : Boolean = false
) extends IBlockMeta {
  /** Сериализация экземпляра этого класса в json-объект. */
  def toPlayJson = JsObject(Seq(
    BLOCK_ID_ESFN -> JsNumber(blockId),
    HEIGHT_ESFN   -> JsNumber(height),
    WIDTH_ESFN    -> JsNumber(width),
    WIDE_ESFN     -> JsBoolean(wide)
  ))
}

