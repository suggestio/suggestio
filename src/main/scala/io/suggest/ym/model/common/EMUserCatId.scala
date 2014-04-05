package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 18:48
 * Description: Поддержка поля userCatId, содержащего id категории.
 */
object EMUserCatId {
  val USER_CAT_ID_ESFN = "userCatId"
}

import EMUserCatId._


trait EMUserCatIdStatic[T <: EMUserCatIdMut[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldString(USER_CAT_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (USER_CAT_ID_ESFN, value) =>
        acc.userCatId = Option(EsModel.stringParser(value))
    }
  }
}


trait EMUserCatId[T <: EMUserCatId[T]] extends EsModelT[T] {

  def userCatId    : Option[String]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (userCatId.isDefined)
      acc.field(USER_CAT_ID_ESFN, userCatId.get)
  }
}

trait EMUserCatIdMut[T <: EMUserCatIdMut[T]] extends EMUserCatId[T] {
  var userCatId: Option[String]
}
