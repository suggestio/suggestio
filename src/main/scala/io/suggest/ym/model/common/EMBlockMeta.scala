package io.suggest.ym.model.common

import io.suggest.model.es.{EsModelStaticMutAkvT, EsModelPlayJsonT, EsModelUtil}
import io.suggest.model.n2.ad.blk.{IBlockMeta, BlockMeta}
import io.suggest.util.SioEsUtil._
import EsModelUtil.FieldsJsonAcc
import play.api.libs.json.{JsBoolean, JsObject, JsNumber}

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
        acc.blockMeta = deserializeBm(blockMetaRaw)
    }
  }


  /** Десериализация BlockMeta из выхлопа jackson. */
  private def deserializeBm(x: Any): BlockMeta = {
    x match {
      case m: java.util.Map[_,_] =>
        import BlockMeta._
        BlockMeta(
          blockId = EsModelUtil.intParser(m get BLOCK_ID_ESFN),
          height  = EsModelUtil.intParser(m get HEIGHT_ESFN),
          width   = Option(m get WIDTH_ESFN)
            .fold(WIDTH_DFLT)(EsModelUtil.intParser),
          wide    = Option(m get WIDE_ESFN)
            .fold(false)(EsModelUtil.booleanParser)
        )
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

/** Аддон для экземпляра [[EsModelPlayJsonT]] для интеграции поля blockId в модель. */
trait EMBlockMeta extends EMBlockMetaI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    BLOCK_META_ESFN -> toPlayJsonBm(blockMeta) :: super.writeJsonFields(acc)
  }

  /** Сериализация экземпляра этого класса в json-объект. */
  private def toPlayJsonBm(bm: IBlockMeta): JsObject = {
    import BlockMeta._
    JsObject(Seq(
      BLOCK_ID_ESFN -> JsNumber(bm.blockId),
      HEIGHT_ESFN   -> JsNumber(bm.height),
      WIDTH_ESFN    -> JsNumber(bm.width),
      WIDE_ESFN     -> JsBoolean(bm.wide)
    ))
  }

}


trait EMBlockMetaMut extends EMBlockMeta {
  override type T <: EMBlockMetaMut
  var blockMeta: BlockMeta
}




