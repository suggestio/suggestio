package io.suggest.ym.model

import io.suggest.model.{EsModelMinimalStaticT, EsModelT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.common._
import org.elasticsearch.common.xcontent.XContentBuilder
import com.fasterxml.jackson.annotation.JsonIgnore
import common._
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 19:16
 * Description: Узел рекламной сети. Он приходит на смену всем MMart, MShop и т.д.
 */
object MANNode
  extends MANNodeBasicStatic[MANNode]
  with EMNameStatic[MANNode]
  with EMCompanyIdStatic[MANNode]
  with EMDateCreatedStatic[MANNode]
  with EMPersonIdsStatic[MANNode]
  with EMLogoImgIdStatic[MANNode]
{
  val ES_TYPE_NAME: String = "anNode"

  protected def dummy(id: String): MANNode = ???
}


/**
 * Трейт для статической стороны модели MANNode.
 * Для нормального stackable trait без подсветки красным цветом везде, надо чтобы была базовая реализация отдельно
 * от целевой реализации. */
trait MANNodeBasicStatic[T <: MANNodeBasic[T]] extends EsModelStaticT[T] {

  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    PartialFunction.empty
  }

  def generateMappingProps: List[DocField] = {
    Nil
  }

  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )
}

/** Трейт базовой реализации экземпляра модели. Вынесен из неё из-за особенностей stackable trait pattern. */
trait MANNodeBasic[T <: MANNodeBasic[T]] extends EsModelT[T] {
  def writeJsonFields(acc: XContentBuilder) {
    // Тут могут быть поля, специфичные для модели. А могут быть в самом классе.
  }
}


case class MANNode(
  var name        : String,
  var companyId   : CompanyId_t,
  var id          : Option[String],
  var personIds   : Set[String],
  var logoImgOpt  : Option[MImgInfo],
  var dateCreated : DateTime = DateTime.now
)
  extends MANNodeBasic[MANNode]
  with EMName[MANNode]
  with EMCompanyId[MANNode]
  with EMDateCreated[MANNode]
  with EMPersonIds[MANNode]
  with EMLogoImg[MANNode]
{

  @JsonIgnore
  def companion: EsModelMinimalStaticT[MANNode] = MANNode

}

