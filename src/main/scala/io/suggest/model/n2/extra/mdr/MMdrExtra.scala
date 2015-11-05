package io.suggest.model.n2.extra.mdr

import io.suggest.common.EmptyProduct
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.{util => ju}


// TODO N2 Эта модель стала частью списка эджей. Нужно её удалить после переключения на N2.


object MMdrExtra extends IGenEsMappingProps {

  val FREE_ADV_ESFN   = "fa"

  override def generateMappingProps: List[DocField] = List(
    FieldObject(FREE_ADV_ESFN, enabled = true, properties = MFreeAdv.generateMappingProps)
  )

  val deserialize: PartialFunction[Any, MMdrExtra] = {
    case jmap: ju.Map[_,_] =>
      MMdrExtra(
        freeAdv = Option(jmap.get(FREE_ADV_ESFN)).map(MFreeAdv.deserialize)
      )
  }

  val empty = MMdrExtra()

  implicit val FORMAT: OFormat[MMdrExtra] = {
    (__ \ FREE_ADV_ESFN).formatNullable[MFreeAdv]
      .inmap [MMdrExtra] (apply, _.freeAdv)
  }

}


/**
 * Инфа по модерации.
 * @param freeAdv Данные по возможности бесплатного размещения карточки (у себя самого, например).
 */
case class MMdrExtra(
  freeAdv: Option[MFreeAdv] = None
)
  extends EmptyProduct
{

  def toPlayJson: JsObject = {
    MMdrExtra.FORMAT
      .writes(this)
  }

}
