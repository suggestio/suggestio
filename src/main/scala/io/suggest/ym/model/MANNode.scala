package io.suggest.ym.model

import io.suggest.model.{EsModelT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.common.{EMNameStatic, EMName}
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 19:16
 * Description: Узел рекламной сети. Он приходит на смену всем MMart, MShop и т.д.
 */
object MANNode extends MANNodeBasicStatic[MANNode] with EMNameStatic[MANNode] {
  val ES_TYPE_NAME: String = "anNode"

  protected def dummy(id: String): MANNode = ???
}

// Для нормального stackable trait без подсветки красным цветом везде, надо чтобы была базовая реализация.

trait MANNodeBasicStatic[T <: MANNodeBasic[T]] extends EsModelStaticT[T] {

  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = PartialFunction.empty

  def generateMappingProps: List[DocField] = Nil

  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )
}

trait MANNodeBasic[T <: MANNodeBasic[T]] extends EsModelT[T] {
  def writeJsonFields(acc: XContentBuilder) {
    ???
  }
}


case class MANNode(
  var name: String,
  var id : Option[String]
) extends MANNodeBasic[MANNode] with EMName[MANNode] {



}
