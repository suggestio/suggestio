package io.suggest.model.n2.tag.edge

import java.{util => ju}

import io.suggest.model.PrefixedFn
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.15 9:37
 * Description: Модель одного указателя на тег из одного узла ADN и N2.
 * По сути это нечеткое ребро графа, способное указывать на несколько тегов одновременно.
 */

object MTagEdge extends PrefixedFn {

  /** Название под-поля тега c поддержкой полнотекстового поиска. */
  val TAG_FACE_FN   = "raw"
  def TAG_FACE_ESFN = _fullFn(TAG_FACE_FN)

  override protected def _PARENT_FN = EMTagsEdge.TAGS_FN


  /** Десериализатор из JSON. */
  implicit val READS: Reads[MTagEdge] = {
    (__ \ TAG_FACE_FN).read[String]
      .map { apply }
  }

  /** Сериализатор в JSON. */
  implicit val WRITES: Writes[MTagEdge] = {
    (__ \ TAG_FACE_FN).write[String]
      .contramap(_.face)
  }


  /** legacy-десериализация из выхлопов jackson'а. */
  def fromJackson(rawMap: Any): MTagEdge = {
    val m = rawMap.asInstanceOf[ ju.Map[String, String] ]
    MTagEdge(
      face = m.get(MTagEdge.TAG_FACE_FN)
    )
  }

  import io.suggest.util.SioEsUtil._

  /** Сборка ES-маппинга для nested-object тегов. Тут перечисляются поля каждого nested-object. */
  def generateMappingProps: List[DocField] = {
    List(
      FieldString(
        id              = TAG_FACE_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = true
      )
    )
  }

  /** Приведение коллекции тегов к итератору будующей карты тегов. */
  def tags2mapIterator(edges: TraversableOnce[MTagEdge]): Iterator[(String, MTagEdge)] = {
    edges
      .toIterator
      .map { t => t.face -> t }
  }
  /** Приведение коллекции тегов к карте. */
  def tags2map(edges: TraversableOnce[MTagEdge]): TagsMap_t = {
    tags2mapIterator(edges)
      .toMap
  }

  /** Приведение карты тегов к последовательности в неопределенном порядке. */
  def map2tags(tmap: TagsMap_t): List[MTagEdge] = {
    tmap.valuesIterator
      .toList
  }

  /** Приведение карты тегов к отсортированному по алфавиту списку. */
  def map2sortedTags(tmap: TagsMap_t): List[MTagEdge] = {
    if (tmap.nonEmpty) {
      map2tags(tmap)
        .sortBy(_.face)
    } else {
      Nil
    }
  }

}


/** Интерфейс экземпляров модели. */
trait ITagEdge {

  /** Человеко-читабельное и понимабельное название тега, почищенное в общих чертах. */
  def face : String

}


/** Дефолтовая реализация модели одного тега узла. */
case class MTagEdge(
  override val face : String
)
  extends ITagEdge
