package io.suggest.model.n2.tag.vertex

import io.suggest.model._
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import MTagFace._

import scala.collection.Map

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 18:22
 * Description: Модель содержимого tagEdge, т.е. tag-свойства одной вершины N2 Graph.
 */

object MTagVertex extends IGenEsMappingProps {

  object Fields {
    object Faces extends PrefixedFn {
      val FACES_FN = "f"
      override protected def _PARENT_FN: String = FACES_FN

      /** Имя поля, хранящего "лицо" тега, т.е. то, как описывают словами этот тег пользователи интернетов. */
      def FACE_NAME_FN = _fullFn( MTagFace.NAME_FN )
    }
  }

  import Fields.Faces.FACES_FN

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNestedObject(FACES_FN, enabled = true, properties = MTagFace.generateMappingProps)
    )
  }

  /** Поддержка JSON для экземпляров модели. */
  implicit val FORMAT: Format[MTagVertex] = {
    (__ \ FACES_FN).formatNullable[TagFacesMap]
      .inmap [TagFacesMap] (
        _ getOrElse Map.empty,
        { tfm => if (tfm.isEmpty) None else Some(tfm) }
      )
      .inmap [MTagVertex] (
        apply,
        _.faces
      )
  }

}


trait ITagVertex {
  /**
   * Любой [[io.suggest.model.n2.node.MNode]]-узел может быть "тегом", но тег обладает многоликостью.
   * Одни юзеры пишут "халат", другие -- "халаты".
   * Одни юзеры пишут "розовая кофточка", другие "кофточка розовая".
   * + могут быть и более сложные различия в написании при полной смысловой эквивалентности.
   * @return Карта "лиц" текущего тега-вершины.
   */
  def faces: TagFacesMap

}


/** Субмодель одного тега в рамках вершины графа N2. */
case class MTagVertex(
  // TODO display name нужно для отображения человеческого названия, not_analyzed для возможности сортировки по нему.
  override val faces       : TagFacesMap   = Map.empty
)
  extends ITagVertex
