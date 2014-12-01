package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModel, EsModelStaticMutAkvT, EsModelPlayJsonT, EsModelCommonStaticT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import play.api.libs.json.JsBoolean
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 18:52
 * Description: Флаг чужих ресурсов. Гасит любые вызовы к удалению ресурсов.
 */

object EMAlienRsc {

  val ALIEN_RSC_ESFN = "arsc"

}


import EMAlienRsc._


/** Аддон к статической части модели, требующей ручной настройки управления ресурсами. */
trait EMAlienRscStatic extends EsModelCommonStaticT with EsModelStaticMutAkvT {
  override type T <: EMAlienRscMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldBoolean(ALIEN_RSC_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (ALIEN_RSC_ESFN, raw) =>
        acc.alienRsc = EsModel.booleanParser(raw)
    }
  }
}


/** Аддон к экземпляру модели, которая требует управления доступом к eraseResources через настраиваемый флаг. */
trait EMAlientRsc extends EsModelPlayJsonT {
  override type T <: EMAlientRsc

  def alienRsc: Boolean

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (alienRsc)
      ALIEN_RSC_ESFN -> JsBoolean(alienRsc) :: acc0
    else
      acc0
  }

  /** Вызывалка стирания ресурсов. Позволяет переопределить логику вызова doEraseResources(). */
  override def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    if (alienRsc)
      Future successful None
    else
      super.eraseResources
  }
}


/** Mutable-версия [[EMAlientRsc]]. */
trait EMAlienRscMut extends EMAlientRsc {
  override type T <: EMAlienRscMut
  var alienRsc: Boolean
}
